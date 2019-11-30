package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.mutiny.Multi;

public class ToSingleFailOnNull<T> implements Function<Multi<T>, Single<T>> {
    private Class<? extends Throwable> exceptionClass;

    ToSingleFailOnNull(Class<? extends Throwable> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    @Override
    public Single<T> apply(Multi<T> multi) {
        multi.onFailure(exceptionClass);
        return Single.fromPublisher(multi);
    }
}
