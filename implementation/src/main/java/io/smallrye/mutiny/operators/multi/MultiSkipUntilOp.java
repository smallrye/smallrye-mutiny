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

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Skips the items from upstream until the passed predicates returns {@code true}.
 *
 * @param <T> the type of item
 */
public final class MultiSkipUntilOp<T> extends AbstractMultiOperator<T, T> {

    private final Predicate<? super T> predicate;

    public MultiSkipUntilOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        upstream.subscribe().withSubscriber(new SkipUntilProcessor<>(actual, predicate));
    }

    static final class SkipUntilProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Predicate<? super T> predicate;
        private boolean gateOpen = false;

        SkipUntilProcessor(MultiSubscriber<? super T> downstream, Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            if (gateOpen) {
                downstream.onItem(t);
                return;
            }

            boolean toBeSkipped;
            try {
                toBeSkipped = predicate.test(t);
            } catch (Throwable e) {
                failAndCancel(e);
                return;
            }

            if (!toBeSkipped) {
                gateOpen = true;
                downstream.onItem(t);
                return;
            }

            request(1);
        }
    }

}
