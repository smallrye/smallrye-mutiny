package io.smallrye.mutiny.converters.uni;

import io.reactivex.rxjava3.core.Completable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class FromCompletable implements UniConverter<Completable, Void> {
    public static final FromCompletable INSTANCE = new FromCompletable();

    private FromCompletable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<Void> from(Completable instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance.toFlowable()));
    }
}
