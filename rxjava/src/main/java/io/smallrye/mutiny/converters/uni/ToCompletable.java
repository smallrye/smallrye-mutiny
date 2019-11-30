package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.mutiny.Uni;

public class ToCompletable<T> implements Function<Uni<T>, Completable> {
    @Override
    public Completable apply(Uni<T> uni) {
        return Completable.fromPublisher(uni.convert().toPublisher());
    }
}
