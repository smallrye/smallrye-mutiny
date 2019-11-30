package io.smallrye.mutiny.converters.multi;

import io.reactivex.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;

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
