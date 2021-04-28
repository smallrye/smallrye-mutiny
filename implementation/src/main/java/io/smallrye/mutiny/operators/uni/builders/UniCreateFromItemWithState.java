package io.smallrye.mutiny.operators.uni.builders;

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
public class UniCreateFromItemWithState<T, S> extends AbstractUni<T> {

    private final Function<S, ? extends T> mapper;
    private final StateHolder<S> holder;

    public UniCreateFromItemWithState(Supplier<S> stateSupplier, Function<S, ? extends T> mapper) {
        this.holder = new StateHolder<>(stateSupplier);
        this.mapper = mapper;
    }

    @Override
    public void subscribe(UniSubscriber<? super T> subscriber) {
        subscriber.onSubscribe(EmptyUniSubscription.DONE);
        S state;
        try {
            state = holder.get();
            // get() throws an NPE is the produced state is null.
        } catch (Throwable err) {
            subscriber.onFailure(err);
            return;
        }

        T item;
        try {
            item = mapper.apply(state);
        } catch (Throwable err) {
            subscriber.onFailure(err);
            return;
        }
        subscriber.onItem(item);
    }
}
