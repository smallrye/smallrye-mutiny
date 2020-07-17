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

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Skips the first N items from upstream.
 * Failures and completions are propagated.
 */
public final class MultiSkipOp<T> extends AbstractMultiOperator<T, T> {

    private final long numberOfItems;

    public MultiSkipOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        if (numberOfItems == 0) {
            upstream.subscribe().withSubscriber(actual);
        } else {
            upstream.subscribe().withSubscriber(new SkipProcessor<>(actual, numberOfItems));
        }
    }

    static final class SkipProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final AtomicLong remaining;

        SkipProcessor(MultiSubscriber<? super T> downstream, long items) {
            super(downstream);
            this.remaining = new AtomicLong(items);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(remaining.get());
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            long r = remaining.getAndDecrement();
            if (r <= 0L) {
                downstream.onItem(t);
            }
            // Other elements are skipped.
        }
    }
}
