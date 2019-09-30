package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Completable;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.adapt.UniConverter;

public class FromCompletable<T> implements UniConverter<Completable, T> {
    @Override
    public Uni<T> from(Completable instance) {
        return Uni.createFrom().publisher(instance.toFlowable());
    }
}
