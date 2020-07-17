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
import static io.smallrye.mutiny.helpers.ParameterValidation.size;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.tuples.Tuple8;

public class MultiItemCombine8<T1, T2, T3, T4, T5, T6, T7, T8> extends MultiItemCombineIterable {

    public MultiItemCombine8(Iterable<Publisher<?>> iterable) {
        super(iterable);
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fire a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombine8}
     */
    @Override
    public MultiItemCombine8<T1, T2, T3, T4, T5, T6, T7, T8> collectFailures() {
        super.collectFailures();
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
     * @return the current {@link MultiItemCombine8}
     */
    @Override
    public MultiItemCombine8<T1, T2, T3, T4, T5, T6, T7, T8> latestItems() {
        super.latestItems();
        return this;
    }

    /**
     * @return the resulting {@link Multi}. The items are combined into a {@link Tuple8 Tuple8&lt;T1, T2, T3, T4, T5, T6, T7,
     *         T8&gt;}.
     */
    public Multi<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> asTuple() {
        return using(Tuple8::of);
    }

    /**
     * Creates the resulting {@link Multi}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Multi}.
     */
    @SuppressWarnings("unchecked")
    public <O> Multi<O> using(Functions.Function8<T1, T2, T3, T4, T5, T6, T7, T8, O> combinator) {
        nonNull(combinator, "combinator");
        return super.combine(args -> {
            size(args, 8, "args");
            return combinator
                    .apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2), (T4) args.get(3),
                            (T5) args.get(4), (T6) args.get(5), (T7) args.get(6), (T8) args.get(7));
        });
    }
}
