package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
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
    @CheckReturnValue
    public <S, T> UniRepeat<T> uni(Supplier<S> stateSupplier, Function<S, Uni<? extends T>> producer) {
        // Decoration happens in "deferred"
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
    @CheckReturnValue
    public <T> UniRepeat<T> uni(Supplier<Uni<? extends T>> uniSupplier) {
        // Decoration happens in "deferred"
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
    @CheckReturnValue
    public <S, T> UniRepeat<T> completionStage(Supplier<S> stateSupplier,
            Function<S, ? extends CompletionStage<? extends T>> producer) {
        Function<S, ? extends CompletionStage<? extends T>> actual = Infrastructure
                .decorate(nonNull(producer, "producer"));
        // stateSupplier decoration happens in `uni`
        return uni(stateSupplier, s -> Uni.createFrom().completionStage(actual.apply(s)));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link CompletionStage}.
     *
     * @param supplier the producer of {@link CompletionStage} called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    @CheckReturnValue
    public <T> UniRepeat<T> completionStage(Supplier<? extends CompletionStage<? extends T>> supplier) {
        nonNull(supplier, "supplier");
        // Decoration happens in `uni`
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
    @CheckReturnValue
    public <S, T> UniRepeat<T> uni(Supplier<S> stateSupplier,
            BiConsumer<S, UniEmitter<? super T>> consumer) {
        BiConsumer<S, UniEmitter<? super T>> actual = Infrastructure
                .decorate(nonNull(consumer, "consumer"));
        return uni(stateSupplier, s -> Uni.createFrom().emitter(e -> actual.accept(s, e)));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Consumer}
     * receiving an {@link UniEmitter} to fire the item or failure.
     *
     * @param consumer the consumer called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    @CheckReturnValue
    public <T> UniRepeat<T> uni(Consumer<UniEmitter<? super T>> consumer) {
        Consumer<UniEmitter<? super T>> actual = Infrastructure.decorate(nonNull(consumer, "consumer"));
        return uni(() -> Uni.createFrom().emitter(actual));
    }

    /**
     * Creates a {@link io.smallrye.mutiny.Multi} by repeating the items fired by the produced {@link Function}
     * producing the items.
     *
     * @param supplier the producer called for every requested repetition
     * @param <T> the type of emitted item
     * @return the object to configure the repetition
     */
    @CheckReturnValue
    public <T> UniRepeat<T> supplier(Supplier<? extends T> supplier) {
        // Decoration happens in `item`
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
    @CheckReturnValue
    public <S, T> UniRepeat<T> supplier(Supplier<S> stateSupplier, Function<S, ? extends T> producer) {
        // Decoration happens in "uni"
        return uni(stateSupplier, s -> Uni.createFrom().item(() -> producer.apply(s)));
    }
}
