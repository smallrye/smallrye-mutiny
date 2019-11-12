package io.smallrye.reactive.operators;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.multi.MultiSignalConsumerOp;

public class MultiOnFailureConsume<T> extends MultiOperator<T, T> {
    private final Consumer<Throwable> callback;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailureConsume(Multi<T> upstream, Consumer<Throwable> callback,
            Predicate<? super Throwable> predicate) {
        super(nonNull(upstream, "upstream"));
        this.callback = nonNull(callback, "callback");
        this.predicate = predicate == null ? x -> true : predicate;
    }

    @Override
    protected Publisher<T> publisher() {
        return new MultiSignalConsumerOp<>(
                upstream(),
                null,
                null,
                failure -> {
                    if (predicate.test(failure)) {
                        callback.accept(failure);
                    }
                },
                null,
                null,
                null);
    }
}
