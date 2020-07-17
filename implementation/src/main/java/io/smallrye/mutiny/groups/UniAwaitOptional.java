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

import java.time.Duration;
import java.util.Optional;

import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.UniBlockingAwait;

/**
 * Likes {@link UniAwait} but wrapping the item event into an {@link Optional}. This optional is empty if the
 * {@link Uni} fires {@code null}.
 *
 * @param <T> the type of the item
 * @see Uni#await()
 */
public class UniAwaitOptional<T> {

    private final Uni<T> upstream;

    public UniAwaitOptional(Uni<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>indefinitely</strong> until a
     * {@code item} event is fired or a {@code failure} event is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item wrapped into an {@link Optional}. If the item is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @return the item from the {@link Uni} wrapped into an {@link Optional}, empty if the {@link Uni} is resolved
     *         with {@code null}
     */
    public Optional<T> indefinitely() {
        return atMost(null);
    }

    /**
     * Subscribes to the {@link Uni} and waits (blocking the caller thread) <strong>at most</strong> the given duration
     * until an item or failure is fired by the upstream uni.
     * <p>
     * If the {@link Uni} fires an item, it returns that item wrapped into an {@link Optional}. If the item is
     * {@code null} the returned optional is empty.
     * If the {@link Uni} fires a failure, the original exception is thrown (wrapped in
     * a {@link java.util.concurrent.CompletionException} it's a checked exception).
     * If the timeout is reached before completion, a {@link TimeoutException} is thrown.
     * <p>
     * Note that each call to this method triggers a new subscription.
     *
     * @param duration the duration, must not be {@code null}, must not be negative or zero.
     * @return the item from the {@link Uni}, potentially {@code null}
     */
    public Optional<T> atMost(Duration duration) {
        return Optional.ofNullable(UniBlockingAwait.await(upstream, duration));
    }

}
