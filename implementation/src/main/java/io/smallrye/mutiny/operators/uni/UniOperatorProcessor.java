package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.EmptyUniSubscription.DONE;

import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public abstract class UniOperatorProcessor<I, O> implements UniSubscriber<I>, UniSubscription {

    protected final UniSubscriber<? super O> downstream;
    protected final AtomicReference<UniSubscription> upstream = new AtomicReference<>();

    public UniOperatorProcessor(UniSubscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    @Override
    public void onSubscribe(UniSubscription subscription) {
        if (upstream.compareAndSet(null, subscription)) {
            downstream.onSubscribe(this);
        } else {
            subscription.cancel();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onItem(I item) {
        UniSubscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onItem((O) item);
        }
    }

    @Override
    public void onFailure(Throwable failure) {
        UniSubscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onFailure(failure);
        } else {
            Infrastructure.handleDroppedException(failure);
        }
    }

    @Override
    public void cancel() {
        UniSubscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED && subscription != DONE) {
            subscription.cancel();
        }
    }

    public boolean isCancelled() {
        return upstream.get() == CANCELLED;
    }
}
