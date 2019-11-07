package io.smallrye.reactive.converters.multi;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;

public class FromObservable<T> implements MultiConverter<Observable<T>, T> {

    public static final FromObservable INSTANCE = new FromObservable();

    private FromObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Observable<T> instance) {
        return Multi.createFrom().publisher(instance.toFlowable(BackpressureStrategy.BUFFER));
    }
}
