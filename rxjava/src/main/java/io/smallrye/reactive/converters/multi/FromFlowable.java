package io.smallrye.reactive.converters.multi;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;

public class FromFlowable<T> implements MultiConverter<Flowable<T>, T> {

    public static final FromFlowable INSTANCE = new FromFlowable();

    private FromFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Flowable<T> instance) {
        return Multi.createFrom().publisher(instance);
    }
}
