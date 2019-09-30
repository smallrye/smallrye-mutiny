package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Single;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.adapt.UniConverter;

public class FromSingle<T> implements UniConverter<Single<T>, T> {

    @Override
    public Uni<T> from(Single<T> instance) {
        return Uni.createFrom().publisher(instance.toFlowable());
    }
}
