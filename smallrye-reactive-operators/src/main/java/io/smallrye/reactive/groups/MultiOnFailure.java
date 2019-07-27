package io.smallrye.reactive.groups;


import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.*;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class MultiOnFailure<T> {

    private final Multi<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public MultiOnFailure(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Multi} invoking the given callback when the upstream {@link Multi} emits a failure.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> peek(Consumer<Throwable> callback) {
        return new MultiOnFailurePeek<>(upstream, nonNull(callback, "callback"), predicate);
    }

    /**
     * Produces a new {@link Multi} invoking the given function when the current {@link Multi} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Multi}
     */
    public Multi<T> mapTo(Function<? super Throwable, ? extends Throwable> mapper) {
        return new MultiMapOnFailure<>(upstream, predicate, mapper);
    }

    public Multi<T> recoverWithResult(T fallback) {
        return recoverWithResult(() -> fallback);
    }

    public Multi<T> recoverWithResult(Supplier<T> supplier) {
        nonNull(supplier, "supplier");
        return recoverWithResult(ignored -> supplier.get());
    }

    public Multi<T> recoverWithResult(Function<? super Throwable, ? extends T> fallback) {
        nonNull(fallback, "fallback");
        return new MultiFlatMapOnFailure<>(upstream, predicate, failure -> {
            T newResult = fallback.apply(failure);
            return Multi.createFrom().result(newResult);
        });
    }

    public Multi<T> recoverWithMulti(Function<? super Throwable, ? extends Multi<? extends T>> fallback) {
        return new MultiFlatMapOnFailure<>(upstream, predicate, nonNull(fallback, "fallback"));
    }

    public Multi<T> recoverWithMulti(Supplier<? extends Multi<? extends T>> supplier) {
        return recoverWithMulti(ignored -> supplier.get());
    }

    public Multi<T> recoverWithMulti(Multi<? extends T> fallback) {
        return recoverWithMulti(() -> fallback);
    }

    public MultiRetry<T> retry() {
        return new MultiRetry<>(upstream, predicate);
    }


}
