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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Takes the n first items emitted by the upstream, cancelling the subscription after that.
 * <p>
 * If n == 0, the subscriber gets completed if the upstream emits the completion, a failure signal, or a first (dropped)
 * item.
 *
 * @param <T> the type of item
 */
public final class MultiTakeOp<T> extends AbstractMultiOperator<T, T> {

    private final long numberOfItems;

    public MultiTakeOp(Multi<? extends T> upstream, long numberOfItems) {
        super(upstream);
        this.numberOfItems = ParameterValidation.positiveOrZero(numberOfItems, "numberOfItems");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "subscriber");
        upstream.subscribe().withSubscriber(new TakeProcessor<>(downstream, numberOfItems));
    }

    static final class TakeProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final long numberOfItems;
        private long remaining;
        private AtomicInteger wip = new AtomicInteger();

        TakeProcessor(MultiSubscriber<? super T> downstream, long numberOfItems) {
            super(downstream);
            this.numberOfItems = numberOfItems;
            this.remaining = numberOfItems;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                if (numberOfItems == 0) {
                    upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                    Subscriptions.complete(downstream);
                } else {
                    downstream.onSubscribe(this);
                }
            } else {
                s.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            if (upstream.get() == Subscriptions.CANCELLED) {
                return;
            }

            long r = remaining;

            if (r == 0) {
                upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                downstream.onCompletion();
                return;
            }

            remaining = --r;
            downstream.onItem(t);
            if (r == 0L) {
                upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                downstream.onCompletion();
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                downstream.onFailure(Subscriptions.getInvalidRequestException());
                return;
            }
            Subscription actual = upstream.get();
            if (wip.compareAndSet(0, 1)) {
                if (n >= this.numberOfItems) {
                    actual.request(Long.MAX_VALUE);
                } else {
                    actual.request(n);
                }
                return;
            }
            actual.request(n);
        }
    }

}
