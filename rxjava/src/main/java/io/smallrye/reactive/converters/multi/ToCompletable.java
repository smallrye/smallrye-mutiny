package io.smallrye.reactive.converters.multi;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.reactive.Multi;

public class ToCompletable<T> implements Function<Multi<T>, Completable> {
    @Override
    public Completable apply(Multi<T> multi) {
        return Completable.fromPublisher(multi);
    }
}
