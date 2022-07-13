package io.smallrye.mutiny.converters.uni;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class FromObservable<T> implements UniConverter<Observable<T>, T> {

    public static final FromObservable INSTANCE = new FromObservable();

    private FromObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Observable<T> instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance.toFlowable(BackpressureStrategy.BUFFER)));
    }
}
