package io.smallrye.mutiny.converters.multi;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;

@SuppressWarnings("rawtypes")
public class FromObservable<T> implements MultiConverter<Observable<T>, T> {

    public static final FromObservable INSTANCE = new FromObservable();

    private FromObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Observable<T> instance) {
        return Multi.createFrom().publisher(AdaptersToFlow.publisher(instance.toFlowable(BackpressureStrategy.BUFFER)));
    }
}
