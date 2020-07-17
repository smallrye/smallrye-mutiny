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
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.MultiCollector;

public class MultiGroupIntoLists<T> {

    private final Multi<T> upstream;

    public MultiGroupIntoLists(Multi<T> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits connected, non-overlapping lists, each of a fixed duration specified by the
     * {@code duration} parameter. If, during the configured time window, no items are emitted by the upstream
     * {@link Multi}, an empty list is emitted by the returned {@link Multi}.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the resulting {@link Multi} emits the current list
     * and propagates the completion event.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param duration the period of time each list collects items before it is emitted and replaced with a new
     *        list. Must be non {@code null} and positive.
     * @return a Multi that emits every {@code duration} with the items emitted by the upstream multi during the time
     *         window.
     */
    public Multi<List<T>> every(Duration duration) {
        return Infrastructure.onMultiCreation(MultiCollector.list(upstream, validate(duration, "duration")));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits lists every {@code size} items.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the produced {@link Multi} emits the current list,
     * and sends the completion event. This last list may not contain {@code size} items. If the upstream {@link Multi}
     * sends the completion event before having emitted any event, the completion event is propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the size of each collected list, must be positive
     * @return a Multi emitting lists of at most {@code size} items from the upstream Multi.
     */
    public Multi<List<T>> of(int size) {
        return Infrastructure.onMultiCreation(MultiCollector.list(upstream, positive(size, "size")));
    }

    /**
     * Creates a {@link Multi} that emits lists of items collected from the observed {@link Multi}.
     * <p>
     * The resulting {@link Multi} emits lists every {@code skip} items, each containing {@code size} items.
     * <p>
     * When the upstream {@link Multi} sends the completion event, the produced {@link Multi} emits the current list,
     * and sends the completion event. This last list may not contain {@code size} items. If the upstream {@link Multi}
     * * sends the completion event before having emitted any event, the completion event is propagated immediately.
     * <p>
     * If the upstream {@link Multi} sends a failure, the failure is propagated immediately.
     *
     * @param size the size of each collected list, must be positive and non-0
     * @param skip the number of items skipped before starting a new list. If {@code skip} and {@code size} are equal,
     *        this operation is similar to {@link #of(int)}. Must be positive and non-0
     * @return a Multi emitting lists for every {@code skip} items from the upstream Multi. Each list contains at most
     *         {@code size} items
     */
    public Multi<List<T>> of(int size, int skip) {
        return Infrastructure.onMultiCreation(
                MultiCollector.list(upstream, positive(size, "size"), positive(skip, "skip")));
    }

}
