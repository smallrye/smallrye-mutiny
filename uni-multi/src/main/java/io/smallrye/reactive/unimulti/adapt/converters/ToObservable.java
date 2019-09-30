package io.smallrye.reactive.unimulti.adapt.converters;

import java.util.function.Function;

import io.reactivex.Observable;
import io.smallrye.reactive.unimulti.Uni;

public class ToObservable<T> implements Function<Uni<T>, Observable<T>> {
    @Override
    public Observable<T> apply(Uni<T> uni) {
        return Observable.fromPublisher(uni.adapt().toPublisher());
    }
}
