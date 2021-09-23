package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class MultiOperatorProcessor<I, O> implements MultiSubscriber<I>, Subscription {

    // Cannot be final, the TCK checks it gets released.
    protected volatile MultiSubscriber<? super O> downstream;
    protected volatile Subscription upstream = null;
    protected AtomicBoolean hasDownstreamCancelled = new AtomicBoolean();

    private static final AtomicReferenceFieldUpdater<MultiOperatorProcessor, Subscription> UPSTREAM_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(MultiOperatorProcessor.class, Subscription.class, "upstream");

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
        return hasDownstreamCancelled.get();
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
        if (hasDownstreamCancelled.compareAndSet(false, true)) {
            cancelUpstream();
            cleanup();
        }
    }

    protected void cancelUpstream() {
        Subscription actual = UPSTREAM_UPDATER.getAndSet(this, CANCELLED);
        if (actual != null && actual != CANCELLED) {
            actual.cancel();
        }
    }

    protected void cleanup() {
        downstream = null;
    }

}
