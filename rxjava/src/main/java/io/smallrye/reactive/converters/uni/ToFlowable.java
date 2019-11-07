package io.smallrye.reactive.converters.uni;

import java.util.function.Function;

import io.reactivex.Flowable;
import io.smallrye.reactive.Uni;

public class ToFlowable<T> implements Function<Uni<T>, Flowable<T>> {
    public static final ToFlowable INSTANCE = new ToFlowable();

    private ToFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Flowable<T> apply(Uni<T> uni) {
        return Flowable.fromPublisher(uni.convert().toPublisher());
    }
}
