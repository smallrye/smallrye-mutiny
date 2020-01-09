package io.smallrye.mutiny.groups;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.UniEmitter;

public class MultiRepetition {

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Uni}.
     * The producer of {@link Uni} receives a shared state.
     *
     * @param stateSupplier the state supplier, must not be {@code null}, must not return {@code null}.
     * @param producer the producer of {@link Uni} called for every requested repetition
     * @param <S> the type of the shared state
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <S, T> UniRepeat<T> uni(Supplier<S> stateSupplier, Function<S, ? extends Uni<? extends T>> producer) {
        Uni<T> upstream = Uni.createFrom().deferred(stateSupplier, producer);
        return new UniRepeat<>(upstream);
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Uni}.
     *
     * @param uniSupplier the producer of {@link Uni} called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <T> UniRepeat<T> uni(Supplier<? extends Uni<? extends T>> uniSupplier) {
        Uni<T> upstream = Uni.createFrom().deferred(uniSupplier);
        return new UniRepeat<>(upstream);
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link CompletionStage}.
     * The producer of {@link CompletionStage} receives a shared state.
     *
     * @param stateSupplier the state producer, must not be {@code null}, must not return {@code null}.
     * @param producer the producer of {@link CompletionStage} called for every requested repetition
     * @param <S> the type of the shared state
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <S, T> UniRepeat<T> completionStage(Supplier<S> stateSupplier,
            Function<S, ? extends CompletionStage<? extends T>> producer) {
        ParameterValidation.nonNull(producer, "producer");
        return uni(stateSupplier, s -> Uni.createFrom().completionStage(producer.apply(s)));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link CompletionStage}.
     *
     * @param supplier the producer of {@link CompletionStage} called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <T> UniRepeat<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        ParameterValidation.nonNull(supplier, "supplier");
        return uni(() -> Uni.createFrom().completionStage(supplier));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link BiConsumer}.
     * The bi-consumer receives the shared state and an {@link UniEmitter} that allow firing the item (or failure).
     *
     * @param stateSupplier the state supplier, must not be {@code null}, must not return {@code null}.
     * @param consumer the consumer called for every requested repetition
     * @param <S> the type of the shared state
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <S, T> UniRepeat<T> uni(Supplier<S> stateSupplier,
            BiConsumer<S, UniEmitter<? super T>> consumer) {
        ParameterValidation.nonNull(consumer, "consumer");
        return uni(stateSupplier, s -> Uni.createFrom().emitter(e -> consumer.accept(s, e)));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Consumer}
     * receiving an {@link UniEmitter} to fire the item or failure.
     *
     * @param consumer the consumer called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <T> UniRepeat<T> uni(Consumer<UniEmitter<? super T>> consumer) {
        ParameterValidation.nonNull(consumer, "consumer");
        return uni(() -> Uni.createFrom().emitter(consumer));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Function}
     * producing the items.
     *
     * @param supplier the producer called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <T> UniRepeat<T> supplier(Supplier<? extends T> supplier) {
        return new UniRepeat<>(Uni.createFrom().item(supplier));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Function}
     * producing the items. The function receives a shared state.
     *
     * @param stateSupplier the state supplier, must not be {@code null}, must not return {@code null}.
     * @param producer the producer called for every requested repetition
     * @param <S> the type of the shared state
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    public <S, T> UniRepeat<T> supplier(Supplier<S> stateSupplier, Function<S, ? extends T> producer) {
        return uni(stateSupplier, s -> Uni.createFrom().item(() -> producer.apply(s)));
    }
}
