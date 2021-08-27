package io.smallrye.mutiny.converters.multi;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;

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
