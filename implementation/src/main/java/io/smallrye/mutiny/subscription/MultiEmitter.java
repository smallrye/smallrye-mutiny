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

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;

/**
 * An object allowing to send signals to the downstream {@link Multi}.
 * {@link Multi} propagates several item event, once a failure or completion event is fired, the other events have
 * no effects.
 * <p>
 * Emitting a {@code null} item is invalid and will cause a failure.
 *
 * @param <T> the expected type of items.
 */
public interface MultiEmitter<T> {

    /**
     * Emits an {@code item} event downstream.
     * <p>
     * Calling this method after a failure or a completion events has no effect.
     *
     * @param item the item, must not be {@code null}
     * @return this emitter, so firing item events can be chained.
     */
    MultiEmitter<T> emit(T item);

    /**
     * Emits a {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #complete()} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void fail(Throwable failure);

    /**
     * Emits a {@code completion} event downstream indicating that no more item will be sent.
     * <p>
     * Calling this method multiple times or after the {@link #fail(Throwable)} method has no effect.
     */
    void complete();

    /**
     * Attaches a @{code termination} event handler invoked when the downstream {@link Subscription} is cancelled,
     * or when the emitter has emitted either a {@code completion} or {@code failure} event.
     * <p>
     * This method allows cleanup resources once the emitter can be disposed (has reached a terminal state).
     *
     * @param onTermination the action to run on termination, must not be {@code null}
     * @return this emitter
     */
    MultiEmitter<T> onTermination(Runnable onTermination);

    /**
     * @return {@code true} if the downstream cancelled the stream or the emitter was terminated (with a completion
     *         or failure events).
     */
    boolean isCancelled();

    /**
     * @return the current outstanding request amount.
     */
    long requested();

}
