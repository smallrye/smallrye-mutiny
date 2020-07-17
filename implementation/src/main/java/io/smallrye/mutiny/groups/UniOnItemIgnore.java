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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

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
        return onItem.transformToUni(ignored -> Uni.createFrom().failure(supplier));
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
        return onItem.transformToUni(ignored -> other);
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
        return onItem.transformToUni(ignored -> supplier.get());
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a default value.
     * Note that if the current {@link Uni} fails, the default value is not used.
     *
     * @param fallback the value to continue with, can be {@code null}
     * @return the new {@link Uni}
     */
    public Uni<T> andContinueWith(T fallback) {
        return onItem.apply(ignored -> fallback);
    }

    /**
     * Ignores the item fired by the current {@link Uni}, and continue with a {@code null} item.
     *
     * @return the new {@link Uni}
     */
    public Uni<Void> andContinueWithNull() {
        return onItem.apply(ignored -> null);
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
        return onItem.apply(ignored -> supplier.get());
    }

}
