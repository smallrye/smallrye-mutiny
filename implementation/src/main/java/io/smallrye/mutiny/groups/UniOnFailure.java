package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOnFailureFlatMap;
import io.smallrye.mutiny.operators.UniOnFailureMap;
import io.smallrye.mutiny.operators.UniOnItemConsume;

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
    public Uni<T> invoke(Consumer<Throwable> callback) {
        return Infrastructure.onUniCreation(
                new UniOnItemConsume<>(upstream, null, nonNull(callback, "callback")));
    }

    /**
     * Produces a new {@link Uni} invoking the given function when the current {@link Uni} propagates a failure. The
     * function can transform the received failure into another exception that will be fired as failure downstream.
     *
     * @param mapper the mapper function, must not be {@code null}, must not return {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> apply(Function<? super Throwable, ? extends Throwable> mapper) {
        return Infrastructure.onUniCreation(new UniOnFailureMap<>(upstream, predicate, mapper));
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
        return Infrastructure.onUniCreation(new UniOnFailureFlatMap<>(upstream, predicate, failure -> {
            T newResult = fallback.apply(failure);
            return Uni.createFrom().item(newResult);
        }));
    }

    public Uni<T> recoverWithUni(Function<? super Throwable, ? extends Uni<? extends T>> fallback) {
        return Infrastructure.onUniCreation(
                new UniOnFailureFlatMap<>(upstream, predicate, nonNull(fallback, "fallback")));
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
