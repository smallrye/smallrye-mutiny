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

import java.util.function.BiFunction;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Scan operator accumulating items of the same type as the upstream.
 * 
 * @param <T> the type of item
 */
public final class MultiScanOp<T> extends AbstractMultiOperator<T, T> {

    private final BiFunction<T, ? super T, T> accumulator;

    public MultiScanOp(Multi<? extends T> upstream, BiFunction<T, ? super T, T> accumulator) {
        super(upstream);
        this.accumulator = ParameterValidation.nonNull(accumulator, "accumulator");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new ScanProcessor<>(downstream, accumulator));
    }

    static final class ScanProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final BiFunction<T, ? super T, T> accumulator;
        private T current;

        ScanProcessor(MultiSubscriber<? super T> downstream, BiFunction<T, ? super T, T> accumulator) {
            super(downstream);
            this.accumulator = accumulator;
        }

        @Override
        public void onItem(T item) {
            if (isDone()) {
                return;
            }

            T result = item;
            if (current != null) {
                try {
                    result = accumulator.apply(current, item);
                } catch (Throwable e) {
                    onFailure(e);
                    return;
                }
                if (result == null) {
                    onFailure(new NullPointerException(ParameterValidation.MAPPER_RETURNED_NULL));
                    return;
                }
            }

            current = result;
            downstream.onItem(item);

        }

        @Override
        public void onFailure(Throwable failure) {
            super.onFailure(failure);
            current = null;
        }

        @Override
        public void onCompletion() {
            super.onCompletion();
            current = null;
        }
    }
}
