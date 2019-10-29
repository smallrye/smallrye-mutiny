package io.smallrye.reactive.adapt.converters;

import java.util.function.Function;

import io.reactivex.Flowable;
import io.smallrye.reactive.Uni;

public class ToFlowable<T> implements Function<Uni<T>, Flowable<T>> {
    @Override
    public Flowable<T> apply(Uni<T> uni) {
        return Flowable.fromPublisher(uni.adapt().toPublisher());
    }
}
