package io.smallrye.reactive.unimulti.adapt.converters;

import java.util.NoSuchElementException;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.reactive.unimulti.Uni;

public class ToSingleFailOnNull<T> implements Function<Uni<T>, Single<T>> {
    @Override
    public Single<T> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.onItem().ifNull().failWith(NoSuchElementException::new).adapt().toPublisher());
    }
}
