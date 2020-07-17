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

/**
 * The back pressure strategies.
 */
public enum BackPressureStrategy {

    /**
     * Buffer the events.
     * While being generally the default, be aware that this strategy may cause {@link OutOfMemoryError} as it uses
     * unbounded buffer.
     */
    BUFFER,

    /**
     * Drop the incoming item events if the downstream is not ready to receive it.
     */
    DROP,

    /**
     * Fire a failure with a {@link BackPressureFailure} when the downstream can't keep up
     */
    ERROR,

    /**
     * Ignore downstream back-pressure requests. Basically it pushes items downstream as they come.
     * <p>
     * This may cause an {@link BackPressureFailure} to be fired when queues get full downstream.
     */
    IGNORE,

    /**
     * Drop the oldest item events from the buffer so the downstream will get only the latest items from upstream.
     */
    LATEST
}
