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
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniDelayOnItem;
import io.smallrye.mutiny.operators.UniDelayUntil;

/**
 * Configures the delay applied to the item emission.
 * It allows delaying the item emitted by the previous {@code Uni} to its downstream.
 *
 * @param <T> the type of item
 */
public class UniOnItemDelay<T> {

    private final Uni<T> upstream;
    private ScheduledExecutorService executor;

    /**
     * Creates a new {@code UniOnItemDelay} instance.
     *
     * @param upstream the upstream uni
     * @param executor the executor, can be {@code null}, if {@code null} used the default worker executor.
     */
    public UniOnItemDelay(Uni<T> upstream, ScheduledExecutorService executor) {
        this.upstream = upstream;
        this.executor = executor == null ? Infrastructure.getDefaultWorkerPool() : executor;
    }

    /**
     * Configures the executor which is used to <i>wait</i> for the delay duration.
     *
     * @param executor the executor, must not be {@code null}
     * @return this {@code UniOnItemDelay}.
     */
    public UniOnItemDelay<T> onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    /**
     * Delays the item emission by a specific duration.
     *
     * @param duration the duration of the delay, must not be {@code null}, must be strictly positive.
     * @return the produced {@link Uni}
     */
    public Uni<T> by(Duration duration) {
        return Infrastructure.onUniCreation(new UniDelayOnItem<>(upstream, duration, executor));
    }

    /**
     * Delays the item emission until the {@link Uni} produced by the given {@link Function} emits an item
     * (potentially {@code null})
     * <p>
     * When the upstream emits its item, the passed function is called. The returned Uni is subscribed using the
     * configured executor. Once this Uni emits its item, the item emitted by upstream is propagated downstream. If
     * the Uni fails, the failure is propagated instead.
     *
     * @param function the function, must not be {@code null}
     * @return the produced {@link Uni}.
     */
    public Uni<T> until(Function<? super T, ? extends Uni<?>> function) {
        return Infrastructure.onUniCreation(new UniDelayUntil<>(upstream, function, executor));
    }

}
