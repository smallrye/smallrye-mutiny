package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniFlatMapOnItem<I, O> extends UniOperator<I, O> {

    private final Function<? super I, ? extends Uni<? extends O>> mapper;

    public UniFlatMapOnItem(Uni<I> upstream, Function<? super I, ? extends Uni<? extends O>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
    }

    public static <I, O> void invokeAndSubstitute(Function<? super I, ? extends Uni<? extends O>> mapper, I input,
            UniSerializedSubscriber<? super O> subscriber,
            FlatMapSubscription flatMapSubscription) {
        Uni<? extends O> outcome;
        try {
            outcome = mapper.apply(input);
            // We cannot call onItem here, as if onItem would throw an exception
            // it would be caught and onFailure would be called. This would be illegal.
        } catch (Throwable e) {
            subscriber.onFailure(e);
            return;
        }

        if (outcome == null) {
            subscriber.onFailure(new NullPointerException(MAPPER_RETURNED_NULL));
        } else {
            UniSubscriber<O> delegate = new UniDelegatingSubscriber<O, O>(subscriber) {
                @Override
                public void onSubscribe(UniSubscription secondSubscription) {
                    flatMapSubscription.replace(secondSubscription);
                }
            };

            outcome.subscribe().withSubscriber(delegate);
        }
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super O> subscriber) {
        FlatMapSubscription flatMapSubscription = new FlatMapSubscription();
        // Subscribe to the source.
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, O>(subscriber) {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                flatMapSubscription.setInitialUpstream(subscription);
                subscriber.onSubscribe(flatMapSubscription);
            }

            @Override
            public void onItem(I item) {
                invokeAndSubstitute(mapper, item, subscriber, flatMapSubscription);
            }

        });
    }

    protected static class FlatMapSubscription implements UniSubscription {

        private final AtomicReference<Subscription> upstream = new AtomicReference<>();

        @Override
        public void cancel() {
            Subscription previous = upstream.getAndSet(EmptyUniSubscription.CANCELLED);
            if (previous != null) {
                // We can call cancelled on CANCELLED, it's a no-op
                previous.cancel();
            }
        }

        void setInitialUpstream(Subscription up) {
            if (!upstream.compareAndSet(null, up)) {
                throw new IllegalStateException("Invalid upstream Subscription state, was expected none but got one");
            }
        }

        void replace(Subscription up) {
            Subscription previous = upstream.getAndSet(up);
            if (previous == null) {
                throw new IllegalStateException("Invalid upstream Subscription state, was expected one but got none");
            } else if (previous == EmptyUniSubscription.CANCELLED) {
                // cancelled was called, cancelling up and releasing reference
                upstream.set(null);
                up.cancel();
            }
            // We don't have to cancel the previous subscription as replace is called once the upstream
            // has emitted an item event, so it's already disposed.
        }
    }
}
