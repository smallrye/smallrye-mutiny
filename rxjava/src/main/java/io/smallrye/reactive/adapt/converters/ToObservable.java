package io.smallrye.reactive.adapt.converters;

import java.util.function.Function;

import io.reactivex.Observable;
import io.smallrye.reactive.Uni;

public class ToObservable<T> implements Function<Uni<T>, Observable<T>> {
    @Override
    public Observable<T> apply(Uni<T> uni) {
        return Observable.fromPublisher(uni.adapt().toPublisher());
    }
}
