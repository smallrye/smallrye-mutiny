package io.smallrye.mutiny.converters.multi;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

@SuppressWarnings("rawtypes")
public class FromSingle<T> implements MultiConverter<Single<T>, T> {

    public static final FromSingle INSTANCE = new FromSingle();

    private FromSingle() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Single<T> instance) {
        return Multi.createFrom().publisher(AdaptersToFlow.publisher(instance.toFlowable()));
    }
}
