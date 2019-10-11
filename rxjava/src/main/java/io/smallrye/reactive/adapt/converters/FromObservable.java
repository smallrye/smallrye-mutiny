package io.smallrye.reactive.adapt.converters;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;

public class FromObservable<T> implements UniConverter<Observable<T>, T> {

    public static final FromObservable INSTANCE = new FromObservable();

    private FromObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Observable<T> instance) {
        return Uni.createFrom().publisher(instance.toFlowable(BackpressureStrategy.BUFFER));
    }
}
