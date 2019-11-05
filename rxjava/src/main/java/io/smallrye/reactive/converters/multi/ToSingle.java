package io.smallrye.reactive.converters.multi;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.reactive.Multi;

public class ToSingle<T> implements Function<Multi<T>, Single<Optional<T>>> {
    public static final ToSingle INSTANCE = new ToSingle();

    private ToSingle() {
        // Avoid direct instantiation
    }

    @Override
    public Single<Optional<T>> apply(Multi<T> multi) {
        return Single.fromPublisher(multi.map(Optional::ofNullable));
    }

}
