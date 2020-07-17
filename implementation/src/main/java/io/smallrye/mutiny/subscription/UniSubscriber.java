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
package io.smallrye.mutiny.subscription;

import java.util.concurrent.Executor;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniSubscribe;

/**
 * Will receive call to {@link #onSubscribe(UniSubscription)} once after passing an instance of this {@link UniSubscriber}
 * to {@link UniSubscribe#withSubscriber(UniSubscriber)} retrieved from {@link Uni#subscribe()}.
 * Unlike Reactive Streams Subscriber, {@link UniSubscriber} does not request items.
 * <p>
 * After subscription, it can receive:
 * <ul>
 * <li>a <em>item</em> event triggering {@link #onItem(Object)}. The item can be {@code null}.</li>
 * <li>a <em>failure</em> event triggering {@link #onFailure(Throwable)} which signals an error state.</li>
 * </ul>
 * <p>
 * Once this subscriber receives an item or failure event, no more events will be received.
 * <p>
 * Note that unlike in Reactive Streams, the value received in {@link #onItem(Object)} can be {@code null}.
 *
 * @param <T> the expected type of item
 */
public interface UniSubscriber<T> {

    /**
     * Event handler called once the subscribed {@link Uni} has taken into account the subscription. The {@link Uni}
     * have triggered the computation of the item.
     *
     * <strong>IMPORTANT:</strong> {@link #onItem(Object)} and {@link #onFailure(Throwable)} would not be called
     * before the invocation of this method.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#runSubscriptionOn(Executor)} has been
     * called</li>
     * <li>Exception: Throwing an exception cancels the subscription, {@link #onItem(Object)} and
     * {@link #onFailure(Throwable)} won't be called</li>
     * </ul>
     *
     * @param subscription the subscription allowing to cancel the computation.
     */
    void onSubscribe(UniSubscription subscription);

    /**
     * Event handler called once the item has been computed by the subscribed {@link Uni}.
     *
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onFailure(Throwable)} is called, this method won't be called.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#emitOn} has been called</li>
     * <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param item the item, may be {@code null}.
     */
    void onItem(T item);

    /**
     * Called if the computation of the item by the subscriber {@link Uni} failed.
     *
     * <strong>IMPORTANT:</strong> this method will be only called once per subscription. If
     * {@link #onItem(Object)} is called, this method won't be called.
     *
     * <ul>
     * <li>Executor: Operate on no particular executor, except if {@link Uni#emitOn} has been called</li>
     * <li>Exception: Throwing an exception cancels the subscription.
     * </ul>
     *
     * @param failure the failure, cannot be {@code null}.
     */
    void onFailure(Throwable failure);

}
