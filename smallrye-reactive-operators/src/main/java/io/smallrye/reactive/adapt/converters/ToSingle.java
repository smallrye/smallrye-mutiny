package io.smallrye.reactive.adapt.converters;

import io.reactivex.Single;
import io.smallrye.reactive.Uni;

import java.util.Optional;
import java.util.function.Function;

public class ToSingle<T> implements Function<Uni<T>, Single<Optional<T>>> {

    @Override
    public Single<Optional<T>> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.map(Optional::ofNullable).adapt().toPublisher());
    }
}
