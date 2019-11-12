package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;

import static io.smallrye.reactive.helpers.Subscriptions.CANCELLED;

public abstract class MultiOperatorSubscriber<I, O> implements Subscriber<I>, Subscription {

    protected final Subscriber<? super O> downstream;
    protected AtomicReference<Subscription> upstream = new AtomicReference<>();

    public MultiOperatorSubscriber(Subscriber<? super O> actual) {
        this.downstream = ParameterValidation.nonNull(actual, "actual");
    }

    protected boolean isDone() {
        return upstream.get() == CANCELLED;
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
    public void onError(Throwable throwable) {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onError(throwable);
            subscription.cancel();
        }
    }

    @Override
    public void onComplete() {
        Subscription subscription = upstream.getAndSet(CANCELLED);
        if (subscription != CANCELLED) {
            downstream.onComplete();
        }
    }

    @Override
    public void request(long numberOfItems) {
        ParameterValidation.positive(numberOfItems, "numberOfItems");
        Subscription subscription = upstream.get();
        if (subscription != CANCELLED) {
            subscription.request(numberOfItems);
        }
    }

    @Override
    public void cancel() {
        Subscriptions.cancel(upstream);
    }
}
