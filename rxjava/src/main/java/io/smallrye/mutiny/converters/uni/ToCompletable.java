package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.reactivex.Completable;
import io.smallrye.mutiny.Uni;

public class ToCompletable<T> implements Function<Uni<T>, Completable> {

    public static final ToCompletable INSTANCE = new ToCompletable();

    private ToCompletable() {
        // Avoid direct instantiation
    }

    @Override
    public Completable apply(Uni<T> uni) {
        return Completable.fromPublisher(uni.convert().toPublisher());
    }
}
