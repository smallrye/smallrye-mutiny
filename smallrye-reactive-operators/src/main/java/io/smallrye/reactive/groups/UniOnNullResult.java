package io.smallrye.reactive.groups;


import io.smallrye.reactive.Uni;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class UniOnNullResult<T> {

    private final Uni<T> upstream;

    public UniOnNullResult(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * If the current {@link Uni} fires {@code null} as result, the produced {@link Uni} emits the passed failure.
     *
     * @param failure the exception to fire if the current {@link Uni} fires {@code null} as result.
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Throwable failure) {
        nonNull(failure, "failure");
        return failWith(() -> failure);
    }

    /**
     * If the current {@link Uni} fires {@code null} as result, the produced {@link Uni} emits a failure produced
     * using the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the failure, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> failWith(Supplier<Throwable> supplier) {
        nonNull(supplier, "supplier");

        return upstream.onResult().mapToUni((result, emitter) -> {
            if (result != null) {
                emitter.result(result);
                return;
            }
            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Exception e) {
                emitter.failure(e);
                return;
            }

            if (throwable == null) {
                emitter.failure(new NullPointerException("The supplier returned `null`"));
            } else {
                emitter.failure(throwable);
            }
        });
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
     * If the current {@link Uni} fires {@code null} as result, the produced {@link Uni} emits the events produced
     * by the {@link Uni} passed as parameter.
     *
     * @param other the unit to switch to, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Uni<? extends T> other) {
        return switchTo(() -> other);
    }

    /**
     * If the current {@link Uni} fires {@code null} as result, the produced {@link Uni} emits the events produced
     * by an {@link Uni} supplied using the passed {@link Supplier}
     *
     * @param supplier the supplier to use to produce the uni, must not be {@code null}, must not return {@code null}s
     * @return the new {@link Uni}
     */
    public Uni<T> switchTo(Supplier<Uni<? extends T>> supplier) {
        nonNull(supplier, "supplier");

        return upstream.onResult().mapToUni(res -> {
            if (res != null) {
                return Uni.createFrom().result(res);
            } else {
                Uni<? extends T> produced;
                try {
                    produced = supplier.get();
                } catch (Exception e) {
                    return Uni.createFrom().failure(e);
                }

                if (produced == null) {
                    return Uni.createFrom().failure(new NullPointerException("The supplier returned `null`"));
                } else {
                    return produced;
                }
            }
        });

    }

    /**
     * Provides a default result if the current {@link Uni} fires {@code null} as result.
     *
     * @param fallback the default result, must not be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(T fallback) {
        nonNull(fallback, "fallback");
        return continueWith(() -> fallback);
    }

    /**
     * Provides a default result if the current {@link Uni} fires {@code null} as result.
     * The new result is supplied by the given {@link Supplier}.
     *
     * @param supplier the supplier to produce the new result, must not be {@code null}, must not produce {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> continueWith(Supplier<? extends T> supplier) {
        nonNull(supplier, "supplier");
        return upstream.onResult().mapToResult(res -> {
            if (res != null) {
                return res;
            }
            T outcome = supplier.get();
            if (outcome == null) {
                throw new NullPointerException("The supplier returned `null`");
            }
            return outcome;
        });
    }

}
