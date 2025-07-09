package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniMemoizeOp<I> extends UniOperator<I, I> implements UniSubscriber<I>, ContextSupport {

    private UniSubscription currentUpstreamSubscription;

    private Context currentContext = Context.empty();

    private enum State {
        INIT,
        WAITING_FOR_UPSTREAM,
        CACHING
    }

    private final BooleanSupplier invalidationRequested;

    private State state = State.INIT;

    private final ReentrantLock internalLock = new ReentrantLock();

    private final ConcurrentLinkedQueue<UniSubscriber<? super I>> awaiters = new ConcurrentLinkedQueue<>();

    private Object cachedResult = null;

    public UniMemoizeOp(Uni<? extends I> upstream) {
        this(upstream, () -> false);
    }

    public UniMemoizeOp(Uni<? extends I> upstream, BooleanSupplier invalidationRequested) {
        super(nonNull(upstream, "upstream"));
        this.invalidationRequested = invalidationRequested;
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        nonNull(subscriber, "subscriber");
        internalLock.lock();
        checkForInvalidation();
        switch (state) {
            case INIT:
                state = State.WAITING_FOR_UPSTREAM;
                awaiters.add(subscriber);
                currentContext = subscriber.context();
                internalLock.unlock();
                subscriber.onSubscribe(new MemoizedSubscription(subscriber));
                upstream().subscribe().withSubscriber(this);
                break;
            case WAITING_FOR_UPSTREAM:
                awaiters.add(subscriber);
                internalLock.unlock();
                subscriber.onSubscribe(new MemoizedSubscription(subscriber));
                break;
            case CACHING:
                internalLock.unlock();
                subscriber.onSubscribe(new MemoizedSubscription(subscriber));
                forwardTo(subscriber);
                break;
        }
    }

    private void checkForInvalidation() {
        if (invalidationRequested.getAsBoolean()) {
            state = State.INIT;
            if (currentUpstreamSubscription != null) {
                currentUpstreamSubscription.cancel();
                currentUpstreamSubscription = null;
            }
        }
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        internalLock.lock();
        this.currentUpstreamSubscription = subscription;
        internalLock.unlock();
    }

    @Override
    public void onItem(I item) {
        internalLock.lock();
        if (state == State.WAITING_FOR_UPSTREAM) {
            state = State.CACHING;
            cachedResult = item;
            internalLock.unlock();
            notifyAwaiters();
        } else {
            internalLock.unlock();
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        internalLock.lock();
        if (state == State.WAITING_FOR_UPSTREAM) {
            state = State.CACHING;
            cachedResult = failure;
            internalLock.unlock();
            notifyAwaiters();
        } else {
            internalLock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private void forwardTo(UniSubscriber<? super I> subscriber) {
        if (cachedResult instanceof Throwable) {
            subscriber.onFailure((Throwable) cachedResult);
        } else {
            subscriber.onItem((I) cachedResult);
        }
    }

    @Override
    public Context context() {
        return currentContext;
    }

    private void notifyAwaiters() {
        UniSubscriber<? super I> awaiter;
        while ((awaiter = awaiters.poll()) != null) {
            forwardTo(awaiter);
        }
    }

    private class MemoizedSubscription implements UniSubscription {

        private final UniSubscriber<? super I> subscriber;

        MemoizedSubscription(UniSubscriber<? super I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            awaiters.remove(subscriber);
        }
    }
}
