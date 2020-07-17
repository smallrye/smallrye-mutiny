/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

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
    public Uni<T> failWith(Supplier<? extends Throwable> supplier) {
        nonNull(supplier, "supplier");

        return Infrastructure.onUniCreation(upstream.onItem().transformToUni((item, emitter) -> {
            if (item != null) {
                emitter.complete(item);
                return;
            }
            Throwable throwable;
            try {
                throwable = supplier.get();
            } catch (Throwable e) {
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

        Uni<T> uni = upstream.onItem().transformToUni(res -> {
            if (res != null) {
                return Uni.createFrom().item(res);
            } else {
                Uni<? extends T> produced;
                try {
                    produced = supplier.get();
                } catch (Throwable e) {
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
        return Infrastructure.onUniCreation(upstream.onItem().apply(res -> {
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
