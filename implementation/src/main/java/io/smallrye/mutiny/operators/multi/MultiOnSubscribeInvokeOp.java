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

import java.util.Objects;
import java.util.function.Consumer;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Execute a given (async) callback on subscription.
 *
 * The subscription is only sent downstream once the action completes successfully (provides an item, potentially
 * {@code null}). If the action fails, the failure is propagated downstream.
 *
 * @param <T> the value type
 */
public final class MultiOnSubscribeInvokeOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super Subscription> onSubscribe;

    public MultiOnSubscribeInvokeOp(Multi<? extends T> upstream,
            Consumer<? super Subscription> onSubscribe) {
        super(upstream);
        this.onSubscribe = onSubscribe;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> actual) {
        OnSubscribeSubscriber subscriber = new OnSubscribeSubscriber(
                Objects.requireNonNull(actual, "Subscriber must not be `null`"));
        upstream.subscribe().withSubscriber(subscriber);
    }

    private final class OnSubscribeSubscriber extends MultiOperatorProcessor<T, T> {

        OnSubscribeSubscriber(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                try {
                    onSubscribe.accept(s);
                } catch (Throwable e) {
                    Subscriptions.fail(downstream, e);
                    upstream.getAndSet(Subscriptions.CANCELLED).cancel();
                    return;
                }
                downstream.onSubscribe(this);
            } else {
                s.cancel();
            }
        }

    }

}
