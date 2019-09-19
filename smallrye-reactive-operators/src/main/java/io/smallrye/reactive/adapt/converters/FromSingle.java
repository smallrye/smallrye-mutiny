package io.smallrye.reactive.adapt.converters;

import io.reactivex.Single;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;

public class FromSingle<T> implements UniConverter<Single<T>, T> {

    @Override
    public Uni<T> from(Single<T> instance) {
        return Uni.createFrom().publisher(instance.toFlowable());
    }
}
