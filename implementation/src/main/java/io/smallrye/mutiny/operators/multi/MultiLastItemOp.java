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

import static io.smallrye.mutiny.helpers.Subscriptions.CANCELLED;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class MultiLastItemOp<T> extends AbstractMultiOperator<T, T> {

    public MultiLastItemOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiLastItemProcessor<T>(downstream));
    }

    static final class MultiLastItemProcessor<T> extends MultiOperatorProcessor<T, T> {

        T last;

        MultiLastItemProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T item) {
            last = item;
        }

        @Override
        public void onFailure(Throwable failure) {
            super.onFailure(failure);
            last = null;
        }

        @Override
        public void onCompletion() {
            Subscription subscription = upstream.getAndSet(CANCELLED);
            if (subscription != CANCELLED) {
                T item = last;
                if (item != null) {
                    last = null; // release before calling the callback.
                    downstream.onItem(item);
                }
                downstream.onCompletion();
            }
        }
    }
}
