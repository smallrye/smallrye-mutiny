package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Completable;
import io.smallrye.reactive.unimulti.Uni;

import java.util.function.Function;

public class ToCompletable<T> implements Function<Uni<T>, Completable> {
    @Override
    public Completable apply(Uni<T> uni) {
        return Completable.fromPublisher(uni.adapt().toPublisher());
    }
}
