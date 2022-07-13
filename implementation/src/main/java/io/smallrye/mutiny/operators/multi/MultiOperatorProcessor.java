package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class MultiOperatorProcessor<I, O> implements MultiSubscriber<I>, Subscription, ContextSupport {

    /*
     * We used to have an interpretation of the RS TCK that it had to be null on cancellation to release the subscriber.
     * It's actually not necessary (and NPE-prone) since operators are instantiated per-subscription, so the *publisher*
     * does not actually keep references on cancelled subscribers.
     */
    protected volatile MultiSubscriber<? super O> downstream;

    protected volatile Subscription upstream = null;
    private volatile int cancellationRequested = 0;

    private static final AtomicReferenceFieldUpdater<MultiOperatorProcessor, Subscription> UPSTREAM_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(MultiOperatorProcessor.class, Subscription.class, "upstream");

    private static final AtomicIntegerFieldUpdater<MultiOperatorProcessor> CANCELLATION_REQUESTED_UPDATER = AtomicIntegerFieldUpdater
            .newUpdater(MultiOperatorProcessor.class, "cancellationRequested");

    public MultiOperatorProcessor(MultiSubscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    void failAndCancel(Throwable throwable) {
        Subscription subscription = getUpstreamSubscription();
        if (subscription != null) {
            subscription.cancel();
        }
        onFailure(throwable);
    }

    protected Subscription getUpstreamSubscription() {
        return upstream;
    }

    protected boolean compareAndSetUpstreamSubscription(Subscription expectedValue, Subscription newValue) {
        return UPSTREAM_UPDATER.compareAndSet(this, expectedValue, newValue);
    }

    protected Subscription getAndSetUpstreamSubscription(Subscription newValue) {
        return UPSTREAM_UPDATER.getAndSet(this, newValue);
    }

    protected boolean isDone() {
        return getUpstreamSubscription() == CANCELLED;
    }

    protected boolean isCancelled() {
        return cancellationRequested == 1;
    }

    @Override
    public Context context() {
        if (downstream instanceof ContextSupport) {
            return ((ContextSupport) downstream).context();
        } else {
            return Context.empty();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (compareAndSetUpstreamSubscription(null, subscription)) {
            // Propagate subscription to downstream.
            downstream.onSubscribe(this);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onFailure(throwable);
        } else {
            Infrastructure.handleDroppedException(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItem(I item) {
        Subscription subscription = getUpstreamSubscription();
        if (subscription != CANCELLED) {
            downstream.onItem((O) item);
        }
    }

    @Override
    public void onCompletion() {
        Subscription subscription = getAndSetUpstreamSubscription(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onCompletion();
        }
    }

    @Override
    public void request(long numberOfItems) {
        Subscription subscription = getUpstreamSubscription();
        if (subscription != CANCELLED) {
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
                return;
            }
            subscription.request(numberOfItems);
        }
    }

    @Override
    public void cancel() {
        if (compareAndSwapDownstreamCancellationRequest()) {
            cancelUpstream();
        }
    }

    protected final boolean compareAndSwapDownstreamCancellationRequest() {
        return CANCELLATION_REQUESTED_UPDATER.compareAndSet(this, 0, 1);
    }

    protected void cancelUpstream() {
        this.cancellationRequested = 1;
        Subscription actual = UPSTREAM_UPDATER.getAndSet(this, CANCELLED);
        if (actual != null && actual != CANCELLED) {
            actual.cancel();
        }
    }
}
