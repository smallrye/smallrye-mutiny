/*
 * Copyright (c) 2011-2017 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.reactive.operators.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import io.smallrye.reactive.helpers.Subscriptions;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Subscribes to the upstream asynchronously using the given executor.
 *
 * @param <T> the type of item
 */
public class MultiSubscribeOnOp<T> extends AbstractMultiWithUpstream<T, T> {

    private final Executor executor;

    public MultiSubscribeOnOp(
            Multi<? extends T> upstream,
            Executor executor) {
        super(upstream);
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        SubscribeOnSubscriber<T> sub = new SubscribeOnSubscriber<>(downstream, executor);
        downstream.onSubscribe(sub);
        sub.scheduleSubscription(upstream, downstream);
    }

    static final class SubscribeOnSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final Executor executor;
        private final AtomicLong requested = new AtomicLong();

        SubscribeOnSubscriber(Subscriber<? super T> downstream, Executor executor) {
            super(downstream);
            this.executor = executor;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                long requests = requested.getAndSet(0L);
                if (requests != 0L) {
                    requestUpstream(requests, subscription);
                }
            } else {
                subscription.cancel();
            }
        }

        void requestUpstream(final long n, final Subscription s) {
            try {
                executor.execute(() -> s.request(n));
            } catch (RejectedExecutionException rejected) {
                super.onError(rejected);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscription subscription = upstream.get();
                if (subscription != null) {
                    requestUpstream(n, subscription);
                } else {
                    // We are not yet subscribed.
                    Subscriptions.add(requested, n);
                    // Check we if are subscribed now
                    subscription = upstream.get();
                    if (subscription != null) {
                        long requests = requested.getAndSet(0L);
                        if (requests != 0L) {
                            requestUpstream(requests, subscription);
                        }
                    }
                }
            }
        }

        void scheduleSubscription(Multi<? extends T> upstream, Subscriber<? super T> downstream) {
            try {
                executor.execute(() -> upstream.subscribe(this));
            } catch (RejectedExecutionException rejected) {
                if (!isDone()) {
                    downstream.onError(rejected);
                }
            }
        }
    }

}
