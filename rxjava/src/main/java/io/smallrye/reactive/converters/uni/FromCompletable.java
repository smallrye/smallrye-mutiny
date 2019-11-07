package io.smallrye.reactive.converters.uni;

import io.reactivex.Completable;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.converters.UniConverter;

public class FromCompletable implements UniConverter<Completable, Void> {
    public static final FromCompletable INSTANCE = new FromCompletable();

    private FromCompletable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<Void> from(Completable instance) {
        return Uni.createFrom().publisher(instance.toFlowable());
    }
}
