package io.smallrye.reactive.converters.uni;

import java.util.NoSuchElementException;
import java.util.function.Function;

import io.reactivex.Single;
import io.smallrye.reactive.Uni;

public class ToSingleFailOnNull<T> implements Function<Uni<T>, Single<T>> {
    public static final ToSingleFailOnNull INSTANCE = new ToSingleFailOnNull();

    private ToSingleFailOnNull() {
        // Avoid direct instantiation
    }

    @Override
    public Single<T> apply(Uni<T> uni) {
        return Single.fromPublisher(uni.onItem().ifNull().failWith(NoSuchElementException::new).convert().toPublisher());
    }
}
