package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.mutiny.Multi;

public class ToCompletable<T> implements Function<Multi<T>, Completable> {
    @Override
    public Completable apply(Multi<T> multi) {
        return Completable.fromPublisher(multi);
    }
}
