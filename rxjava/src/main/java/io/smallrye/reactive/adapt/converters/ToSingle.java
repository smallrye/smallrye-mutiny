package io.smallrye.reactive.adapt.converters;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.reactive.Uni;

public class ToSingle<T> implements Function<Uni<T>, Single<Optional<T>>> {

    public static <R> ToSingleWithDefault<R> withDefault(R defaultValue) {
        return new ToSingleWithDefault<>(defaultValue);
    }

    public static <R> ToSingleFailOnNull<R> failOnNull() {
        return new ToSingleFailOnNull<>();
    }

    @Override
    public Single<Optional<T>> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.map(Optional::ofNullable).adapt().toPublisher());
    }

}
