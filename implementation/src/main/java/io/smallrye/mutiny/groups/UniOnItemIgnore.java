package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniOnItemIgnore<T> {

    private final UniOnItem<T> onItem;

    public UniOnItemIgnore(UniOnItem<T> onItem) {
        this.onItem = nonNull(onItem, "onItem");
    }

    /**
     * Ignores the item fired by the current {@link Uni} and fails with the passed failure.
     *
     * @param failure the exception to propagate
     * @return the new {@link Uni}
     */
    @CheckReturnValue
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
    @CheckReturnValue
    public Uni<T> andFail(Supplier<Throwable> supplier) {
        Supplier<Throwable> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return onItem.transformToUni(ignored -> Uni.createFrom().failure(actual.get()));
    }

    /**
     * Like {@link #andFail(Throwable)} but using an {@link Exception}.
     *
     * @return the new {@link Uni}
     */
    @CheckReturnValue
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
    @CheckReturnValue
    public <O> Uni<O> andSwitchTo(Uni<? extends O> other) {
        nonNull(other, "other");
        return onItem.transformToUni(ignored -> other);
    }

    /**
     * Ignores the item fired by the current {@link Uni} and continue with the {@link Uni} produced by the given supplier.
     *
     * @param supplier the supplier to produce the new {@link Uni}, must not be {@code null}, must not produce {@code null}
     * @param <O> the type of the new Uni
     * @return the new Uni
     */
    @CheckReturnValue
    public <O> Uni<O> andSwitchTo(Supplier<Uni<? extends O>> supplier) {
        nonNull(supplier, "supplier");
        return onItem.transformToUni(ignored -> supplier.get());
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a default value.
     * Note that if the current {@link Uni} fails, the default value is not used.
     *
     * @param fallback the value to continue with, can be {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> andContinueWith(T fallback) {
        return onItem.transform(ignored -> fallback);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a {@code null} item.
     *
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<Void> andContinueWithNull() {
        return onItem.transform(ignored -> null);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with the value produced by the given supplier.
     * Note that if the current {@link Uni} fails, the supplier is not used.
     *
     * @param supplier the default value, must not be {@code null}, can produce {@code null}
     * @return the new {@link Uni}
     */
    @CheckReturnValue
    public Uni<T> andContinueWith(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return onItem.transform(ignored -> actual.get());
    }

}
