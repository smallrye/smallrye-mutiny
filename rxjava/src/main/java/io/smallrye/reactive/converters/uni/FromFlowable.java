package io.smallrye.reactive.converters.uni;

import io.reactivex.Flowable;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.converters.UniConverter;

public class FromFlowable<T> implements UniConverter<Flowable<T>, T> {

    public static final FromFlowable INSTANCE = new FromFlowable();

    private FromFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Flowable<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
