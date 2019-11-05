package io.smallrye.reactive.converters.multi;

import io.reactivex.Completable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;

public class FromCompletable implements MultiConverter<Completable, Void> {
    public static final FromCompletable INSTANCE = new FromCompletable();

    private FromCompletable() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<Void> from(Completable instance) {
        return Multi.createFrom().publisher(instance.toFlowable());
    }
}
