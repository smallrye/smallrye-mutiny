package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public abstract class UniOperatorProcessor<I, O> implements UniSubscriber<I>, UniSubscription {

    protected final UniSubscriber<? super O> downstream;

    private static final AtomicReferenceFieldUpdater<UniOperatorProcessor, UniSubscription> updater = AtomicReferenceFieldUpdater
            .newUpdater(UniOperatorProcessor.class, UniSubscription.class, "upstream");

    private volatile UniSubscription upstream;

    public UniOperatorProcessor(UniSubscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        if (updater.compareAndSet(this, null, subscription)) {
            downstream.onSubscribe(this);
        } else {
            subscription.cancel();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onItem(I item) {
        UniSubscription subscription = updater.getAndSet(this, CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onItem((O) item);
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        UniSubscription subscription = updater.getAndSet(this, CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onFailure(failure);
        } else {
            Infrastructure.handleDroppedException(failure);
        }
    }

    @Override
    public void cancel() {
        UniSubscription subscription = updater.getAndSet(this, CANCELLED);
        if (subscription != null && subscription != CANCELLED && subscription != DONE) {
            subscription.cancel();
        }
    }

    public boolean isCancelled() {
        return upstream == CANCELLED;
    }

    protected final UniSubscription getCurrentUpstreamSubscription() {
        return upstream;
    }

    protected final UniSubscription getAndSetUpstreamSubscription(UniSubscription newValue) {
        return updater.getAndSet(this, newValue);
    }

    protected final boolean compareAndSetUpstreamSubscription(UniSubscription expect, UniSubscription update) {
        return updater.compareAndSet(this, expect, update);
    }

}
