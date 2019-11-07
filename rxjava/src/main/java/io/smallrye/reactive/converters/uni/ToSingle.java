package io.smallrye.reactive.converters.uni;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.reactive.Uni;

public class ToSingle<T> implements Function<Uni<T>, Single<Optional<T>>> {
    public static final ToSingle INSTANCE = new ToSingle();

    private ToSingle() {
        // Avoid direct instantiation
    }

    public static <R> ToSingleWithDefault<R> withDefault(R defaultValue) {
        return new ToSingleWithDefault<>(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <R> ToSingleFailOnNull<R> failOnNull() {
        return ToSingleFailOnNull.INSTANCE;
    }

    @Override
    public Single<Optional<T>> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.map(Optional::ofNullable).convert().toPublisher());
    }

}
