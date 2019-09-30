package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.adapt.UniConverter;

public class FromFlowable<T> implements UniConverter<Flowable<T>, T> {
    @Override
    public Uni<T> from(Flowable<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
