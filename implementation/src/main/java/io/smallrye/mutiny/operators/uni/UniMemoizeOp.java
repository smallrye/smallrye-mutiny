package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.*;
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

    private final List<UniSubscriber<? super I>> awaiters = new ArrayList<>();

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

        boolean shouldSubscribeUpstream = false;
        Object cached = null;
        boolean wasInCachingState = false;

        internalLock.lock();
        try {
            checkForInvalidation(); // May throw an exception
            switch (state) {
                case INIT:
                    state = State.WAITING_FOR_UPSTREAM;
                    awaiters.add(subscriber);
                    currentContext = subscriber.context();
                    shouldSubscribeUpstream = true;
                    break;
                case WAITING_FOR_UPSTREAM:
                    awaiters.add(subscriber);
                    break;
                case CACHING:
                    cached = cachedResult;
                    wasInCachingState = true;
                    break;
            }
        } finally {
            internalLock.unlock();
        }

        subscriber.onSubscribe(new MemoizedSubscription(subscriber));

        if (shouldSubscribeUpstream) {
            upstream().subscribe().withSubscriber(this);
        } else if (wasInCachingState) {
            forwardTo(subscriber, cached);
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
        List<UniSubscriber<? super I>> toNotify = null;
        if (state == State.WAITING_FOR_UPSTREAM) {
            state = State.CACHING;
            cachedResult = item;
            toNotify = gatherAwaiters();
        }
        internalLock.unlock();
        if (toNotify != null) {
            notifyAwaiters(toNotify, item);
        }
    }

    private List<UniSubscriber<? super I>> gatherAwaiters() {
        ArrayList<UniSubscriber<? super I>> copy = new ArrayList<>(awaiters);
        awaiters.clear();
        return copy;
    }

    private void notifyAwaiters(List<UniSubscriber<? super I>> toNotify, Object result) {
        Iterator<UniSubscriber<? super I>> iterator = toNotify.iterator();
        while (iterator.hasNext()) {
            UniSubscriber<? super I> awaiter = iterator.next();
            forwardTo(awaiter, result);
            iterator.remove();
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        internalLock.lock();
        List<UniSubscriber<? super I>> toNotify = null;
        if (state == State.WAITING_FOR_UPSTREAM) {
            state = State.CACHING;
            cachedResult = failure;
            toNotify = gatherAwaiters();
        }
        internalLock.unlock();
        if (toNotify != null) {
            notifyAwaiters(toNotify, failure);
        }
    }

    @SuppressWarnings("unchecked")
    private void forwardTo(UniSubscriber<? super I> subscriber, Object result) {
        if (result instanceof Throwable) {
            subscriber.onFailure((Throwable) result);
        } else {
            subscriber.onItem((I) result);
        }
    }

    @Override
    public Context context() {
        return currentContext;
    }

    private class MemoizedSubscription implements UniSubscription {

        private final UniSubscriber<? super I> subscriber;

        MemoizedSubscription(UniSubscriber<? super I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            internalLock.lock();
            try {
                awaiters.remove(subscriber);
            } finally {
                internalLock.unlock();
            }
        }
    }
}
