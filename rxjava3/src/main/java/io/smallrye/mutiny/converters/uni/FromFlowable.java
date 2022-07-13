package io.smallrye.mutiny.converters.uni;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class FromFlowable<T> implements UniConverter<Flowable<T>, T> {

    public static final FromFlowable INSTANCE = new FromFlowable();

    private FromFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Flowable<T> instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance));
    }
}
