package io.smallrye.reactive.groups;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniFlatMapOnFailure;
import io.smallrye.reactive.operators.UniMapOnFailure;
import io.smallrye.reactive.operators.UniOnEventConsume;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

public class UniOnFailure<T> {

    private final Uni<T> upstream;
    private final Predicate<? super Throwable> predicate;

    public UniOnFailure(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        this.upstream = upstream;
        this.predicate = predicate == null ? x -> true : predicate;
    }

    /**
     * Produces a new {@link Uni} invoking the given callback when this {@link Uni} emits a failure.
     *
     * @param callback the callback, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> consume(Consumer<Throwable> callback) {
        return new UniOnEventConsume<>(upstream, null, nonNull(callback, "callback"));
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> mapTo(Function<? super Throwable, ? extends Throwable> mapper) {
        return new UniMapOnFailure<>(upstream, predicate, mapper);
    }

    public Uni<T> recoverWithItem(T fallback) {
        return recoverWithItem(() -> fallback);
    }

    public Uni<T> recoverWithItem(Supplier<T> supplier) {
        nonNull(supplier, "supplier");
        return recoverWithItem(ignored -> supplier.get());
    }

    public Uni<T> recoverWithItem(Function<? super Throwable, ? extends T> fallback) {
        nonNull(fallback, "fallback");
        return new UniFlatMapOnFailure<>(upstream, predicate, failure -> {
            T newResult = fallback.apply(failure);
            return Uni.createFrom().item(newResult);
        });
    }

    public Uni<T> recoverWithUni(Function<? super Throwable, ? extends Uni<? extends T>> fallback) {
        return new UniFlatMapOnFailure<>(upstream, predicate, nonNull(fallback, "fallback"));
    }

    public Uni<T> recoverWithUni(Supplier<? extends Uni<? extends T>> supplier) {
        return recoverWithUni(ignored -> supplier.get());
    }

    public Uni<T> recoverWithUni(Uni<? extends T> fallback) {
        return recoverWithUni(() -> fallback);
    }

    public UniRetry<T> retry() {
        return new UniRetry<>(upstream, predicate);
    }

}
