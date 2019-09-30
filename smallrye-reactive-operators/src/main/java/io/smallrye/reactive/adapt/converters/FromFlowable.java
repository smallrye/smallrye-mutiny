package io.smallrye.reactive.adapt.converters;

import io.reactivex.Flowable;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;

public class FromFlowable<T> implements UniConverter<Flowable<T>, T> {
    @Override
    public Uni<T> from(Flowable<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
