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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOrCombination;

public class UniAny {

    public static final UniAny INSTANCE = new UniAny();

    private UniAny() {
        // avoid direct instantiation.
    }

    /**
     * Like {@link #of(Iterable)} but with an array of {@link Uni} as parameter
     *
     * @param unis the array, must not be {@code null}, must not contain @{code null}
     * @param <T> the type of item emitted by the different unis.
     * @return the produced {@link Uni}
     */
    @SafeVarargs
    public final <T> Uni<T> of(Uni<? super T>... unis) {
        List<Uni<? super T>> list = Arrays.asList(nonNull(unis, "unis"));
        return Infrastructure.onUniCreation(new UniOrCombination<>(list));
    }

    /**
     * Creates a {@link Uni} forwarding the first event (item or failure). It behaves like the fastest
     * of these competing unis. If the passed iterable is empty, the resulting {@link Uni} gets a {@code null} item
     * just after subscription.
     * <p>
     * This method subscribes to the set of {@link Uni}. When one of the {@link Uni} fires an item or a failure
     * a failure, the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the event of the selected
     * {@link Uni}. Use {@link Uni#emitOn(Executor)} to change that thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis}
     * contained in the {@code iterable} are also cancelled.
     *
     * @param iterable a set of {@link Uni}, must not be {@code null}.
     * @param <T> the type of item emitted by the different unis.
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> of(Iterable<? extends Uni<? super T>> iterable) {
        return Infrastructure.onUniCreation(new UniOrCombination<>(iterable));
    }
}
