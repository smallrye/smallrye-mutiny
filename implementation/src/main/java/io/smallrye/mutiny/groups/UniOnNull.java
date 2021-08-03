package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniOnNull<T> {

    private final UniOnPredicate<T> uniOnPredicate;

    public UniOnNull(Uni<T> upstream) {
        this.uniOnPredicate = new UniOnPredicate<>(nonNull(upstream, "upstream"), Objects::isNull);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the passed failure.
     *
     * @param failure the exception to fire if the current {@link Uni} fires {@code null} as item.
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Throwable failure) {
        return uniOnPredicate.failWith(failure);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits a failure produced
     * using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Supplier<? extends Throwable> supplier) {
        return uniOnPredicate.failWith(supplier);
    }

    /**
     * Like {@link #failWith(Throwable)} but using a {@link NoSuchElementException}.
     *
     * @return the new {@link Uni}
     */
    public Uni<T> fail() {
        return uniOnPredicate.failWith(NoSuchElementException::new);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the events produced
     * by the {@link Uni} passed as parameter.
     *
     * @param other the unit to switch to, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Uni<? extends T> other) {
        nonNull(other, "other");
        return uniOnPredicate.transformToUniElse(ignore -> other, item -> Uni.createFrom().item(item));
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the events produced
     * by an {@link Uni} supplied using the passed {@link Supplier}
     *
     * @param supplier the supplier to use to produce the uni, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Supplier<Uni<? extends T>> supplier) {
        Supplier<Uni<? extends T>> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return uniOnPredicate.transformToUniElse(ignore -> actual.get(), item -> Uni.createFrom().item(item));
    }

    /**
     * Provides a default item if the current {@link Uni} fires {@code null} as item.
     *
     * @param fallback the default item, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(T fallback) {
        nonNull(fallback, "fallback");
        return uniOnPredicate.transformElse(ignore -> fallback, item -> item);
    }

    /**
     * Provides a default item if the current {@link Uni} fires {@code null} as item.
     * The new item is supplied by the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the new item, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(Supplier<? extends T> supplier) {
        Supplier<? extends T> actual = Infrastructure.decorate(nonNull(supplier, "supplier"));
        return uniOnPredicate.transformElse(ignore -> actual.get(), item -> item);
    }

}
