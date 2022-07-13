package io.smallrye.mutiny.converters.multi;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

@SuppressWarnings("rawtypes")
public class FromFlowable<T> implements MultiConverter<Flowable<T>, T> {

    public static final FromFlowable INSTANCE = new FromFlowable();

    private FromFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Flowable<T> instance) {
        return Multi.createFrom().publisher(AdaptersToFlow.publisher(instance));
    }
}
