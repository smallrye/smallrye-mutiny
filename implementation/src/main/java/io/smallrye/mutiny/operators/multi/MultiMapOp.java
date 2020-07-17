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
package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiMapOp<T, U> extends AbstractMultiOperator<T, U> {
    private final Function<? super T, ? extends U> mapper;

    public MultiMapOp(Multi<T> upstream, Function<? super T, ? extends U> mapper) {
        super(upstream);
        this.mapper = ParameterValidation.nonNull(mapper, "mapper");
    }

    @Override
    public void subscribe(MultiSubscriber<? super U> downstream) {
        if (downstream == null) {
            throw new NullPointerException("Subscriber is `null`");
        }
        upstream.subscribe().withSubscriber(new MapProcessor<T, U>(downstream, mapper));
    }

    static class MapProcessor<I, O> extends MultiOperatorProcessor<I, O> {
        private final Function<? super I, ? extends O> mapper;

        MapProcessor(MultiSubscriber<? super O> actual, Function<? super I, ? extends O> mapper) {
            super(actual);
            this.mapper = mapper;
        }

        @Override
        public void onItem(I item) {
            if (isDone()) {
                return;
            }
            O v;
            try {
                v = mapper.apply(item);
            } catch (Throwable ex) {
                failAndCancel(ex);
                return;
            }
            if (v == null) {
                failAndCancel(new NullPointerException(MAPPER_RETURNED_NULL));
            } else {
                downstream.onItem(v);
            }
        }
    }

}
