package io.smallrye.reactive.groups;

import static io.smallrye.reactive.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.infrastructure.Infrastructure;

public class UniOnNull<T> {

    private final Uni<T> upstream;

    public UniOnNull(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the passed failure.
     *
     * @param failure the exception to fire if the current {@link Uni} fires {@code null} as item.
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Throwable failure) {
        nonNull(failure, "failure");
        return failWith(() -> failure);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits a failure produced
     * using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Supplier<Throwable> supplier) {
        nonNull(supplier, "supplier");

        return Infrastructure.onUniCreation(upstream.onItem().mapToUni((item, emitter) -> {
            if (item != null) {
                emitter.complete(item);
                return;
            }
            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Exception e) {
                emitter.fail(e);
                return;
            }

            if (throwable == null) {
                emitter.fail(new NullPointerException(SUPPLIER_PRODUCED_NULL));
            } else {
                emitter.fail(throwable);
            }
        }));
    }

    /**
     * Like {@link #failWith(Throwable)} but using a {@link NoSuchElementException}.
     *
     * @return the new {@link Uni}
     */
    public Uni<T> fail() {
        return failWith(NoSuchElementException::new);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the events produced
     * by the {@link Uni} passed as parameter.
     *
     * @param other the unit to switch to, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Uni<? extends T> other) {
        return switchTo(() -> other);
    }

    /**
     * If the current {@link Uni} fires {@code null} as item, the produced {@link Uni} emits the events produced
     * by an {@link Uni} supplied using the passed {@link Supplier}
     *
     * @param supplier the supplier to use to produce the uni, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Supplier<Uni<? extends T>> supplier) {
        nonNull(supplier, "supplier");

        Uni<T> uni = upstream.onItem().mapToUni(res -> {
            if (res != null) {
                return Uni.createFrom().item(res);
            } else {
                Uni<? extends T> produced;
                try {
                    produced = supplier.get();
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }

                if (produced == null) {
                    return Uni.createFrom().failure(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                } else {
                    return produced;
                }
            }
        });
        return Infrastructure.onUniCreation(uni);
    }

    /**
     * Provides a default item if the current {@link Uni} fires {@code null} as item.
     *
     * @param fallback the default item, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(T fallback) {
        nonNull(fallback, "fallback");
        return continueWith(() -> fallback);
    }

    /**
     * Provides a default item if the current {@link Uni} fires {@code null} as item.
     * The new item is supplied by the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the new item, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(Supplier<? extends T> supplier) {
        nonNull(supplier, "supplier");
        return Infrastructure.onUniCreation(upstream.onItem().mapToItem(res -> {
            if (res != null) {
                return res;
            }
            T outcome = supplier.get();
            if (outcome == null) {
                throw new NullPointerException(SUPPLIER_PRODUCED_NULL);
            }
            return outcome;
        }));
    }

}
