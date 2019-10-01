package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.subscription.UniSubscriber;
import io.smallrye.reactive.subscription.UniSubscription;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static io.smallrye.reactive.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniCallSubscribeOn<I> extends UniOperator<I, I> {

    private final Executor executor;

    public UniCallSubscribeOn(Uni<? extends I> upstream, Executor executor) {
        super(nonNull(upstream, "upstream"));
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        SubscribeOnUniSubscriber downstream = new SubscribeOnUniSubscriber(subscriber);
        try {
            executor.execute(downstream);
        } catch (Exception e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
        }

    }

    class SubscribeOnUniSubscriber extends UniDelegatingSubscriber<I, I>
            implements Runnable, UniSubscriber<I>, UniSubscription {

        final AtomicReference<UniSubscription> subscription = new AtomicReference<>();

        SubscribeOnUniSubscriber(UniSerializedSubscriber<? super I> actual) {
            super(actual);
        }

        @Override
        public void run() {
            upstream().subscribe().withSubscriber(this);
        }

        @Override
        public void onSubscribe(UniSubscription s) {
            if (subscription.compareAndSet(null, s)) {
                super.onSubscribe(this);
            }
        }

        @Override
        public void cancel() {
            UniSubscription upstream = subscription.getAndSet(CANCELLED);
            if (upstream != null && upstream != CANCELLED) {
                upstream.cancel();
            }
        }
    }
}
