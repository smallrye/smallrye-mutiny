package io.smallrye.reactive.adapt.converters;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.reactive.Uni;

public class ToCompletable<T> implements Function<Uni<T>, Completable> {
    @Override
    public Completable apply(Uni<T> uni) {
        return Completable.fromPublisher(uni.adapt().toPublisher());
    }
}
