package io.smallrye.mutiny.converters.multi;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.uni.UniRxConverters;

public class ToSingle<T> implements Function<Multi<T>, Single<Optional<T>>> {
    public static final ToSingle INSTANCE = new ToSingle();

    private ToSingle() {
        // Avoid direct instantiation
    }

    public <R> ToSingleFailOnNull<R> onEmptyThrow(Supplier<? extends Throwable> supplier) {
        return new ToSingleFailOnNull<>(supplier);
    }

    @Override
    public Single<Optional<T>> apply(Multi<T> multi) {
        return multi.collectItems().first().convert().with(UniRxConverters.toSingle());
    }

}
