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

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

/**
 * Multi operator re-subscribing to the upstream if if receives a failure event.
 * It can re-subscribe indefinitely (passing Long.MAX_VALUE as number of attempts) or a fixed number of times.
 *
 * @param <T> the type of item
 */
public final class MultiRetryOp<T> extends AbstractMultiOperator<T, T> {

    private final long times;

    public MultiRetryOp(Multi<? extends T> upstream, long times) {
        super(upstream);
        this.times = times;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        RetrySubscriber<T> subscriber = new RetrySubscriber<>(upstream, downstream, times);

        downstream.onSubscribe(subscriber);

        if (!subscriber.isCancelled()) {
            subscriber.resubscribe();
        }
    }

    static final class RetrySubscriber<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Publisher<? extends T> upstream;
        private final AtomicInteger wip = new AtomicInteger();

        private long remaining;
        long produced;

        RetrySubscriber(Publisher<? extends T> upstream, MultiSubscriber<? super T> downstream, long attempts) {
            super(downstream);
            this.upstream = upstream;
            this.remaining = attempts;
        }

        @Override
        public void onItem(T t) {
            produced++;
            downstream.onItem(t);
        }

        @Override
        public void onFailure(Throwable t) {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                if (r == 0) {
                    // Forward
                    downstream.onFailure(t);
                    return;
                }
                remaining = r - 1;
            }
            resubscribe();
        }

        void resubscribe() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (isCancelled()) {
                        return;
                    }
                    long c = produced;
                    if (c != 0L) {
                        produced = 0L;
                        emitted(c);
                    }
                    upstream.subscribe(Infrastructure.onMultiSubscription(upstream, this));
                } while (wip.decrementAndGet() != 0);
            }
        }
    }
}
