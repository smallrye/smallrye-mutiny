package io.smallrye.mutiny.converters.uni;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class FromSingle<T> implements UniConverter<Single<T>, T> {

    public static final FromSingle INSTANCE = new FromSingle();

    private FromSingle() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Single<T> instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance.toFlowable()));
    }
}
