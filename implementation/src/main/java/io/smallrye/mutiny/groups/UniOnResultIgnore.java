package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

public class UniOnResultIgnore<T> {

    private final UniOnItem<T> onResult;

    public UniOnResultIgnore(UniOnItem<T> onResult) {
        this.onResult = nonNull(onResult, "onItem");
    }

    /**
     * Ignores the item fired by the current {@link Uni} and fails with the passed failure.
     *
     * @param failure the exception to propagate
     * @return the new {@link Uni}
     */
    public Uni<T> andFail(Throwable failure) {
        nonNull(failure, "failure");
        return andFail(() -> failure);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and fails with a failure produced using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andFail(Supplier<Throwable> supplier) {
        nonNull(supplier, "supplier");
        return onResult.produceUni(ignored -> Uni.createFrom().failure(supplier));
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
     * Ignores the item fired by the current {@link Uni} and continue with the given {@link Uni}.
     *
     * @param other the uni to continue with, must not be {@code null}
     * @param <O> the type of the new Uni
     * @return the new Uni
     */
    public <O> Uni<O> andSwitchTo(Uni<? extends O> other) {
        nonNull(other, "other");
        return onResult.produceUni(ignored -> other);
    }

    /**
     * Ignores the item fired by the current {@link Uni} and continue with the {@link Uni} produced by the given supplier.
     *
     * @param supplier the supplier to produce the new {@link Uni}, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of the new Uni
     * @return the new Uni
     */
    public <O> Uni<O> andSwitchTo(Supplier<Uni<? extends O>> supplier) {
        nonNull(supplier, "supplier");
        return onResult.produceUni(ignored -> supplier.get());
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a default value.
     * Note that if the current {@link Uni} fails, the default value is not used.
     *
     * @param fallback the value to continue with, can be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andContinueWith(T fallback) {
        return onResult.apply(ignored -> fallback);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a {@code null} item.
     *
     * @return the new {@link Uni}
     */
    public Uni<Void> andContinueWithNull() {
        return onResult.apply(ignored -> null);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with the value produced by the given supplier.
     * Note that if the current {@link Uni} fails, the supplier is not used.
     *
     * @param supplier the default value, must not be {@code null}, can produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andContinueWith(Supplier<? extends T> supplier) {
        nonNull(supplier, "supplier");
        return onResult.apply(ignored -> supplier.get());
    }

}
