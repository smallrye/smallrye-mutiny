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
package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.RejectedExecutionException;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Subscribes to the upstream asynchronously using the given executor.
 *
 * @param <T> the type of item
 */
public class MultiSubscribeOnOp<T> extends AbstractMultiOperator<T, T> {

    private final Executor executor;

    public MultiSubscribeOnOp(
            Multi<? extends T> upstream,
            Executor executor) {
        super(upstream);
        this.executor = ParameterValidation.nonNull(executor, "executor");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        SubscribeOnProcessor<T> sub = new SubscribeOnProcessor<>(downstream, executor);
        sub.scheduleSubscription(upstream, downstream);
    }

    static final class SubscribeOnProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final Executor executor;

        SubscribeOnProcessor(MultiSubscriber<? super T> downstream, Executor executor) {
            super(downstream);
            this.executor = executor;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (compareAndSetUpstreamSubscription(null, subscription)) {
                downstream.onSubscribe(this);
            } else {
                subscription.cancel();
            }
        }

        void requestUpstream(final long n, final Subscription s) {
            try {
                executor.execute(() -> s.request(n));
            } catch (RejectedExecutionException rejected) {
                super.onFailure(rejected);
            }
        }

        void scheduleSubscription(Multi<? extends T> upstream, Flow.Subscriber<? super T> downstream) {
            try {
                executor.execute(() -> upstream.subscribe().withSubscriber(this));
            } catch (RejectedExecutionException rejected) {
                if (!isDone()) {
                    downstream.onError(rejected);
                }
            }
        }

        @Override
        public void onItem(T t) {
            downstream.onItem(t);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscription subscription = getUpstreamSubscription();
                requestUpstream(n, subscription);
            }
        }
    }

}
