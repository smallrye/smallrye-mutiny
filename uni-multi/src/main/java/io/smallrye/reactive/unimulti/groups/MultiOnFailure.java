package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.operators.MultiFlatMapOnFailure;
import io.smallrye.reactive.unimulti.operators.MultiMapOnFailure;
import io.smallrye.reactive.unimulti.operators.MultiOnFailureConsume;

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
    public Multi<T> consume(Consumer<Throwable> callback) {
        return new MultiOnFailureConsume<>(upstream, nonNull(callback, "callback"), predicate);
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

    public Multi<T> recoverWithItem(T fallback) {
        nonNull(fallback, "fallback");
        return recoverWithItem(() -> fallback);
    }

    public Multi<T> recoverWithItem(Supplier<T> supplier) {
        nonNull(supplier, "supplier");
        return recoverWithItem(ignored -> {
            T t = supplier.get();
            if (t == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            } else {
                return t;
            }
        });
    }

    public Multi<T> recoverWithCompletion() {
        return recoverWithMulti(Multi.createFrom().empty());
    }

    public Multi<T> recoverWithItem(Function<? super Throwable, ? extends T> fallback) {
        nonNull(fallback, "fallback");
        return new MultiFlatMapOnFailure<>(upstream, predicate, failure -> {
            T newResult = fallback.apply(failure);
            return Multi.createFrom().item(newResult);
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
