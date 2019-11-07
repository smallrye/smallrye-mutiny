package io.smallrye.reactive.converters.multi;

import io.reactivex.Single;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;

public class FromSingle<T> implements MultiConverter<Single<T>, T> {

    public static FromSingle INSTANCE = new FromSingle();

    private FromSingle() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Single<T> instance) {
        return Multi.createFrom().publisher(instance.toFlowable());
    }
}
