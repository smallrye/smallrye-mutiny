package io.smallrye.reactive.converters.uni;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.reactive.Uni;

public class ToCompletable<T> implements Function<Uni<T>, Completable> {
    @Override
    public Completable apply(Uni<T> uni) {
        return Completable.fromPublisher(uni.convert().toPublisher());
    }
}
