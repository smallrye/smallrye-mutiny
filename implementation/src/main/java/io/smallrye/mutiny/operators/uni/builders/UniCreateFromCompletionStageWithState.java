package io.smallrye.mutiny.operators.uni.builders;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is produced from a supplier.
 * This variant of {@link UniCreateFromItemSupplier} accepts a state supplier.
 * The supplied item can be {@code null}.
 *
 * @param <T> the type of the item
 * @param <S> the type of the state
 */
public class UniCreateFromCompletionStageWithState<T, S> extends AbstractUni<T> {

    private final Function<S, ? extends CompletionStage<? extends T>> mapper;
    private final StateHolder<S> holder;

    public UniCreateFromCompletionStageWithState(Supplier<S> stateSupplier,
            Function<S, ? extends CompletionStage<? extends T>> mapper) {
        this.mapper = mapper;
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

        CompletionStage<? extends T> stage;
        try {
            stage = mapper.apply(state);
            Objects.requireNonNull(stage, "The produced CompletionStage is `null`");
        } catch (Throwable err) {
            subscriber.onSubscribe(EmptyUniSubscription.DONE);
            subscriber.onFailure(err);
            return;
        }

        new UniCreateFromCompletionStage.CompletionStageUniSubscription<T>(subscriber, stage).forward();
    }
}
