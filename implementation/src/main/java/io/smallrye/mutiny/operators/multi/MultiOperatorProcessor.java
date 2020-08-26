package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public abstract class MultiOperatorProcessor<I, O> implements MultiSubscriber<I>, Subscription {

    protected final MultiSubscriber<? super O> downstream;
    protected AtomicReference<Subscription> upstream = new AtomicReference<>();
    AtomicBoolean hasDownstreamCancelled = new AtomicBoolean();

    public MultiOperatorProcessor(MultiSubscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    void failAndCancel(Throwable throwable) {
        Subscription subscription = upstream.get();
        if (subscription != null) {
            subscription.cancel();
        }
        onFailure(throwable);
    }

    protected boolean isDone() {
        return upstream.get() == CANCELLED;
    }

    protected boolean isCancelled() {
        return hasDownstreamCancelled.get();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (upstream.compareAndSet(null, subscription)) {
            // Propagate subscription to downstream.
            downstream.onSubscribe(this);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onFailure(throwable);
        } else {
            Infrastructure.handleDroppedException(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onItem(I item) {
        Subscription subscription = upstream.get();
        if (subscription != CANCELLED) {
            downstream.onItem((O) item);
        }
    }

    @Override
    public void onCompletion() {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onCompletion();
        }
    }

    @Override
    public void request(long numberOfItems) {
        Subscription subscription = upstream.get();
        if (subscription != CANCELLED) {
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("Invalid number of request, must be greater than 0"));
            }
            subscription.request(numberOfItems);
        }
    }

    @Override
    public void cancel() {
        if (hasDownstreamCancelled.compareAndSet(false, true)) {
            Subscriptions.cancel(upstream);
        }
    }

}
