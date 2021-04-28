package io.smallrye.mutiny.operators.uni.builders;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is produced from a supplier.
 * This variant of {@link UniCreateFromItemSupplier} accepts a state supplier.
 * The supplied item can be {@code null}.
 *
 * @param <T> the type of the item
 * @param <S> the type of the state
 */
public class UniCreateFromEmitterWithState<T, S> extends AbstractUni<T> {

    private final BiConsumer<S, UniEmitter<? super T>> consumer;
    private final StateHolder<S> holder;

    public UniCreateFromEmitterWithState(Supplier<S> stateSupplier, BiConsumer<S, UniEmitter<? super T>> consumer) {
        this.consumer = consumer;
        this.holder = new StateHolder<>(stateSupplier);
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        S state;
        try {
            state = holder.get();
            // get() throws an NPE is the produced state is null.
        } catch (Throwable err) {
            subscriber.onSubscribe(EmptyUniSubscription.DONE);
            subscriber.onFailure(err);
            return;
        }

        DefaultUniEmitter<? super T> emitter = new DefaultUniEmitter<>(subscriber);
        subscriber.onSubscribe(emitter);
        try {
            consumer.accept(state, emitter);
        } catch (Throwable err) {
            // we use the emitter to be sure that if the failure happens after the first event being fired, it
            // will be dropped.
            emitter.fail(err);
        }

    }
}
