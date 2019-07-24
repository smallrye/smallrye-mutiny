package io.smallrye.reactive.groups;


import io.smallrye.reactive.Uni;

import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class UniOnResultIgnore<T> {

    private final UniOnResult<T> onResult;

    public UniOnResultIgnore(UniOnResult<T> onResult) {
        this.onResult = nonNull(onResult, "onResult");
    }

    /**
     * Ignores the result fired by the current {@link Uni} and fails with the passed failure.
     *
     * @param failure the exception to propagate
     * @return the new {@link Uni}
     */
    public Uni<T> andFail(Throwable failure) {
        nonNull(failure, "failure");
        return andFail(() -> failure);
    }

    /**
     * Ignores the result fired by the current {@link Uni}, and fails with a failure produced using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andFail(Supplier<Throwable> supplier) {
        nonNull(supplier, "supplier");
        return onResult.mapToUni(ignored -> Uni.createFrom().failure(supplier));
    }

    /**
     * Like {@link #andFail(Throwable)} but using an {@link Exception}.
     *
     * @return the new {@link Uni}
     */
    public Uni<T> andFail() {
        return andFail(new Exception("Ignored and Failed"));
    }

    /**
     * Ignores the result fired by the current {@link Uni} and continue with the given {@link Uni}.
     *
     * @param other the uni to continue with, must not be {@code null}
     * @param <O> the type of the new Uni
     * @return the new Uni
     */
    public <O> Uni<O> andSwitchTo(Uni<? extends O> other) {
        nonNull(other, "other");
        return onResult.mapToUni(ignored -> other);
    }

    /**
     * Ignores the result fired by the current {@link Uni} and continue with the {@link Uni} produced by the given supplier.
     *
     * @param supplier the supplier to produce the new {@link Uni}, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of the new Uni
     * @return the new Uni
     */
    public <O> Uni<O> andSwitchTo(Supplier<Uni<? extends O>> supplier) {
        nonNull(supplier, "supplier");
        return onResult.mapToUni(ignored -> supplier.get());
    }

    /**
     * Ignores the result fired by the current {@link Uni}, and continue with a default value.
     * Note that if the current {@link Uni} fails, the default value is not used.
     *
     * @param fallback the value to continue with, can be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andContinueWith(T fallback) {
        return onResult.mapToResult(ignored -> fallback);
    }

    /**
     * Ignores the result fired by the current {@link Uni}, and continue with a {@code null} result.
     *
     * @return the new {@link Uni}
     */
    public Uni<Void> andContinueWithNull() {
        return onResult.mapToResult(ignored -> null);
    }

    /**
     * Ignores the result fired by the current {@link Uni}, and continue with the value produced by the given supplier.
     * Note that if the current {@link Uni} fails, the supplier is not used.
     *
     * @param supplier the default value, must not be {@code null}, can produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andContinueWith(Supplier<? extends T> supplier) {
        nonNull(supplier, "supplier");
        return onResult.mapToResult(ignored -> supplier.get());
    }

}
