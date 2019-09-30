package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Uni;

import java.util.function.Function;

public class ToFlowable<T> implements Function<Uni<T>, Flowable<T>> {
    @Override
    public Flowable<T> apply(Uni<T> uni) {
        return Flowable.fromPublisher(uni.adapt().toPublisher());
    }
}
