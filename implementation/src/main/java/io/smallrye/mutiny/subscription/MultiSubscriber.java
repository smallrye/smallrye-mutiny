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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link Subscriber} receiving calls to {@link #onSubscribe(Subscription)} once after passing an instance of
 * {@link Subscriber} to {@link Publisher#subscribe(Subscriber)}.
 * <p>
 * No further events will be received until {@link Subscription#request(long)} is called.
 * <p>
 * After signaling demand:
 * <ul>
 * <li>One or more invocations of {@link #onItem(Object)} up to the maximum number defined by
 * {@link Subscription#request(long)}</li>
 * <li>Single invocation of {@link #onFailure(Throwable)} or {@link #onCompletion()} which signals a terminal state after which
 * no further events will be sent.
 * </ul>
 * <p>
 * Demand can be signaled via {@link Subscription#request(long)} whenever the {@link Subscriber} instance is capable of handling
 * more.
 *
 * This interface bridges the Mutiny model and the Reactive Streams model.
 *
 * @param <T> the type of item.
 */
public interface MultiSubscriber<T> extends Subscriber<T> {

    /**
     * Method called when the upstream emits an {@code item} event, in response to to requests to
     * {@link Subscription#request(long)}.
     *
     * @param item the item, must not be {@code null}.
     */
    void onItem(T item);

    /**
     * Method called when the upstream emits a {@code failure} terminal event.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     *
     * @param failure the failure, must not be {@code null}.
     */
    void onFailure(Throwable failure);

    /**
     * Method called when the upstream emits a {@code completion} terminal event.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     */
    void onCompletion();

    /**
     * Data notification sent by the {@link Publisher} in response to requests to {@link Subscription#request(long)}.
     * Delegates to {@link #onItem(Object)}
     *
     * @param t the element signaled
     */
    default void onNext(T t) {
        onItem(t);
    }

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     * Delegates to {@link #onFailure(Throwable)}
     *
     * @param t the throwable signaled
     */
    default void onError(Throwable t) {
        onFailure(t);
    }

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     * Delegates to {@link #onCompletion()}
     */
    default void onComplete() {
        onCompletion();
    }

}
