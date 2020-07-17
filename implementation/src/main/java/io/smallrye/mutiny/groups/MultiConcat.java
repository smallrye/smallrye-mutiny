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

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiConcatOp;

/**
 * Creates new {@link Multi} by concatenating several {@link Multi} or {@link Publisher}.
 * This class allows configuring how the concatenation is executed. Unlike a merge, a concatenation emits the items in
 * order. Streams are read one-by-one and the items are emitted in this order.
 */
public class MultiConcat {

    private boolean collectFailures;

    public MultiConcat(boolean collectFailures) {
        this.collectFailures = collectFailures;
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}.
     *
     * @param publishers the publishers, must not be empty, must not contain {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} using a concatenation
     */
    @SafeVarargs
    public final <T> Multi<T> streams(Publisher<T>... publishers) {
        return Infrastructure.onMultiCreation(new MultiConcatOp<>(collectFailures, publishers));
    }

    /**
     * Creates a new {@link Multi} concatenating the items emitted by the given {@link Multi multis} /
     * {@link Publisher publishers}..
     *
     * @param iterable the publishers, must not be empty, must not contain {@code null}, must not be {@code null}
     * @param <T> the type of item
     * @return the new {@link Multi} emitting the items from the given set of {@link Multi} using a concatenation
     */
    public <T> Multi<T> streams(Iterable<? extends Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        iterable.forEach(list::add);
        //noinspection unchecked
        return Infrastructure.onMultiCreation(new MultiConcatOp<>(collectFailures, list.toArray(new Publisher[0])));
    }

    /**
     * Indicates that the concatenation process should not propagate the first receive failure, but collect them until
     * all the items from all (non-failing) participants have been emitted. Then, the failures are propagated downstream
     * (as a {@link CompositeException} if several failures have been received).
     *
     * @return this {@link MultiConcat} configured to collect the failures.
     */
    public MultiConcat collectFailures() {
        this.collectFailures = true;
        return this;
    }

}
