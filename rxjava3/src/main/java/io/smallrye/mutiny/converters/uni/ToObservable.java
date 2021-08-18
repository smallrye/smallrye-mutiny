package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.reactivex.rxjava3.core.Observable;
import io.smallrye.mutiny.Uni;

public class ToObservable<T> implements Function<Uni<T>, Observable<T>> {
    public static final ToObservable INSTANCE = new ToObservable();

    private ToObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Observable<T> apply(Uni<T> uni) {
        return Observable.fromPublisher(uni.convert().toPublisher());
    }
}
