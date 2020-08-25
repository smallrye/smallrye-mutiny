package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniRunSubscribeOn<I> extends UniOperator<I, I> {

    private final Executor executor;

    public UniRunSubscribeOn(Uni<? extends I> upstream, Executor executor) {
        super(nonNull(upstream, "upstream"));
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        SubscribeOnUniSubscriber downstream = new SubscribeOnUniSubscriber(subscriber);
        try {
            executor.execute(downstream);
        } catch (Throwable e) {
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
            try {
                AbstractUni.subscribe(upstream(), this);
            } catch (Exception e) {
                super.onSubscribe(CANCELLED);
                super.onFailure(e);
            }
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
