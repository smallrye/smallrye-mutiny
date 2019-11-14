package io.smallrye.reactive.operators.multi;

import static io.smallrye.reactive.helpers.Subscriptions.CANCELLED;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;

@SuppressWarnings("SubscriberImplementation")
public abstract class MultiOperatorProcessor<I, O> implements Subscriber<I>, Subscription {

    protected final Subscriber<? super O> downstream;
    protected AtomicReference<Subscription> upstream = new AtomicReference<>();
    protected AtomicBoolean cancelled = new AtomicBoolean();

    public MultiOperatorProcessor(Subscriber<? super O> downstream) {
        this.downstream = ParameterValidation.nonNull(downstream, "downstream");
    }

    protected boolean isDone() {
        return upstream.get() == CANCELLED;
    }

    protected boolean isCancelled() {
        return cancelled.get();
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
        if (cancelled.compareAndSet(false, true)) {
            Subscriptions.cancel(upstream);
        }
    }
}
