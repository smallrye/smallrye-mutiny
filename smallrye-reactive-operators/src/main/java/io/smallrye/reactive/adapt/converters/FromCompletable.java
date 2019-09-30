package io.smallrye.reactive.adapt.converters;

import io.reactivex.Completable;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;

public class FromCompletable<T> implements UniConverter<Completable, T> {
    @Override
    public Uni<T> from(Completable instance) {
        return Uni.createFrom().publisher(instance.toFlowable());
    }
}
