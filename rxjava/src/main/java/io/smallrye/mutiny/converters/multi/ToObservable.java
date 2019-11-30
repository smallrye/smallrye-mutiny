package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.reactivex.Observable;
import io.smallrye.mutiny.Multi;

public class ToObservable<T> implements Function<Multi<T>, Observable<T>> {
    public static final ToObservable INSTANCE = new ToObservable();

    private ToObservable() {
        // Avoid direct instantiation
    }

    @Override
    public Observable<T> apply(Multi<T> multi) {
        return Observable.fromPublisher(multi);
    }
}
