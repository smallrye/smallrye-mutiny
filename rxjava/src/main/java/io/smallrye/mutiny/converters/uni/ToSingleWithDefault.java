package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.mutiny.Uni;

public class ToSingleWithDefault<T> implements Function<Uni<T>, Single<T>> {

    private T defaultValue;

    public ToSingleWithDefault(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Single<T> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.onItem().ifNull().continueWith(defaultValue).convert().toPublisher());
    }
}
