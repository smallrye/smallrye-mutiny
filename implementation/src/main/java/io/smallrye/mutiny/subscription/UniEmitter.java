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

import io.smallrye.mutiny.Uni;

/**
 * An object allowing to send signals to the downstream {@link Uni}.
 * {@link Uni} propagates a single item event, once the first is propagated, the others events have no effect.
 *
 * @param <T> the expected type of item.
 */
public interface UniEmitter<T> {

    /**
     * Emits the {@code item} event downstream with the given (potentially {@code null}) item.
     * <p>
     * Calling this method multiple times or after the {@link #fail(Throwable)} method has no effect.
     *
     * @param item the item, may be {@code null}
     */
    void complete(T item);

    /**
     * Emits the {@code failure} event downstream with the given exception.
     * <p>
     * Calling this method multiple times or after the {@link #complete(Object)} method has no effect.
     *
     * @param failure the exception, must not be {@code null}
     */
    void fail(Throwable failure);

    /**
     * Attaches a @{code termination} event handler invoked when the downstream {@link UniSubscription} is cancelled,
     * or when the emitter has emitted either an {@code item} or {@code failure} event.
     * <p>
     * This method allows cleanup resources once the emitter can be disposed (has reached a terminal state).
     *
     * @param onTermination the action to run on termination, must not be {@code null}
     * @return this emitter
     */
    UniEmitter<T> onTermination(Runnable onTermination);

}
