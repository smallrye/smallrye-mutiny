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

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniCallSubscribeOn<I> extends UniOperator<I, I> {

    private final Executor executor;

    public UniCallSubscribeOn(Uni<? extends I> upstream, Executor executor) {
        super(nonNull(upstream, "upstream"));
        this.executor = nonNull(executor, "executor");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super I> subscriber) {
        SubscribeOnUniSubscriber downstream = new SubscribeOnUniSubscriber(subscriber);
        try {
            executor.execute(downstream);
        } catch (Throwable e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
        }

    }

    class SubscribeOnUniSubscriber extends UniDelegatingSubscriber<I, I>
            implements Runnable, UniSubscriber<I>, UniSubscription {

        final AtomicReference<UniSubscription> subscription = new AtomicReference<>();

        SubscribeOnUniSubscriber(UniSerializedSubscriber<? super I> actual) {
            super(actual);
        }

        @Override
        public void run() {
            AbstractUni.subscribe(upstream(), this);
        }

        @Override
        public void onSubscribe(UniSubscription s) {
            if (subscription.compareAndSet(null, s)) {
                super.onSubscribe(this);
            }
        }

        @Override
        public void cancel() {
            UniSubscription upstream = subscription.getAndSet(CANCELLED);
            if (upstream != null && upstream != CANCELLED) {
                upstream.cancel();
            }
        }
    }
}
