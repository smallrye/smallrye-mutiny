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
package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiCombineLatestOp;
import io.smallrye.mutiny.operators.multi.MultiZipOp;

public class MultiItemCombineIterable {

    private boolean collectFailures;
    private boolean latest;

    private Iterable<? extends Publisher<?>> iterable;

    public MultiItemCombineIterable(Iterable<? extends Publisher<?>> iterable) {
        this.iterable = iterable;
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fire a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    public MultiItemCombineIterable collectFailures() {
        this.collectFailures = true;
        return this;
    }

    /**
     * By default, the combination logic is called with one item of each observed stream. It <em>waits</em> until
     * all the observed streams emit an item and call the combination logic. In other words, it associated the items
     * from different stream having the same <em>index</em>. If one of the stream completes, the produced stream also
     * completes.
     *
     * <p>
     * With this method, you can change this behavior and call the combination logic every time one of one of the observed
     * streams emit an item. It would call the combination logic with this new item and the latest items emitted by the
     * other streams. It wait until all the streams have emitted at least an item before calling the combination logic.
     * <p>
     * If one of the stream completes before having emitted a value, the produced streams also completes without emitting
     * a value.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    public MultiItemCombineIterable latestItems() {
        this.latest = true;
        return this;
    }

    /**
     * Sets the combination logic as parameter and returns a {@link Multi} associating the items from the observed
     * stream using this combinator.
     *
     * @param combinator the combination function, must not be {@code null}
     * @param <O> the type of item produced by the returned {@link Multi} (the return type of the combinator)
     * @return the new {@link Multi}
     */
    public <O> Multi<O> using(Function<List<?>, O> combinator) {
        nonNull(combinator, "combinator");
        return combine(combinator);
    }

    <O> Multi<O> combine(Function<List<?>, ? extends O> combinator) {
        if (latest) {
            if (collectFailures) {
                return Infrastructure.onMultiCreation(new MultiCombineLatestOp<>(iterable, combinator, 128, true));
            } else {
                return Infrastructure.onMultiCreation(new MultiCombineLatestOp<>(iterable, combinator, 128, false));
            }
        } else {
            if (collectFailures) {
                return Infrastructure.onMultiCreation(new MultiZipOp<>(iterable, combinator, 128, true));
            } else {
                return Infrastructure.onMultiCreation(new MultiZipOp<>(iterable, combinator, 128, false));
            }
        }
    }
}
