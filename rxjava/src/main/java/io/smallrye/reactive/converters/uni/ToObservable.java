package io.smallrye.reactive.converters.uni;

import java.util.function.Function;

import io.reactivex.Observable;
import io.smallrye.reactive.Uni;

public class ToObservable<T> implements Function<Uni<T>, Observable<T>> {
    @Override
    public Observable<T> apply(Uni<T> uni) {
        return Observable.fromPublisher(uni.convert().toPublisher());
    }
}
