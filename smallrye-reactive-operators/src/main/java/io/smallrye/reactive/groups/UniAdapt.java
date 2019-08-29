package io.smallrye.reactive.groups;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniAdaptTo;
import io.smallrye.reactive.adapt.converters.ToCompletable;
import io.smallrye.reactive.adapt.converters.ToMaybe;
import io.smallrye.reactive.adapt.converters.ToSingle;
import io.smallrye.reactive.operators.UniToPublisher;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.function.Function;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniAdapt<T> {

    private final Uni<T> upstream;

    public UniAdapt(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Uni} into an instance of the given class. The transformations acts as follows:
     * <ol>
     * <li>If this is an instance of O - return this</li>
     * <li>If O has a {@code fromPublisher} method, this method is called with a {@link Publisher} produced
     * using {@link #toPublisher()}</li>
     * <li>If O has a {@code instance} method, this method is called with a {@link Publisher} produced
     * using {@link #toPublisher()}</li>
     * </ol>
     *
     * @param clazz the output class
     * @param <O>   the produced type
     * @return an instance of O
     * @throws RuntimeException if the conversion fails.
     */
    public <O> O to(Class<O> clazz) {
        return new UniAdaptTo<>(upstream, nonNull(clazz, "clazz")).adapt();
    }

    public <R> R with(Function<Uni<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    public Single<Optional<T>> toSingle() {
        return with(new ToSingle<>());
    }

    public Completable toCompletable() {
        return with(new ToCompletable<>());
    }

    public Maybe<T> toMaybe() {
        return with(new ToMaybe<>());
    }

    public Publisher<T> toPublisher() {
        return UniToPublisher.adapt(upstream);
    }

}
