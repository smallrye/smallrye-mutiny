package io.smallrye.reactive.groups;

import io.reactivex.*;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniToPublisher;
import io.smallrye.reactive.adapt.converters.*;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniAdapt<T> {

    private final Uni<T> upstream;

    public UniAdapt(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Transforms this {@link Uni} into a type using the provided converter.
     *
     * @param converter the converter function
     * @return an instance of O
     * @throws RuntimeException if the conversion fails.
     */
    public <R> R with(Function<Uni<T>, R> converter) {
        nonNull(converter, "converter");
        return converter.apply(upstream);
    }

    public Single<Optional<T>> toSingle() {
        return with(new ToSingle<>());
    }

    public Single<T> toSingle(T defaultValue) {
        nonNull(defaultValue, "defaultValue");
        return with(new ToSingleWithDefault<>(defaultValue));
    }

    public Completable toCompletable() {
        return with(new ToCompletable<>());
    }

    public Maybe<T> toMaybe() {
        return with(new ToMaybe<>());
    }

    public Observable<T> toObservable() {
        return with(new ToObservable<>());
    }

    public Flowable<T> toFlowable() {
        return with(new ToFlowable<>());
    }

    public CompletionStage<T> toCompletionStage() {
        return with(new ToCompletionStage<>());
    }

    public CompletableFuture<T> toCompletableFuture() {
        return with(new ToCompletableFuture<>());
    }

    public Publisher<T> toPublisher() {
        return UniToPublisher.adapt(upstream);
    }

}
