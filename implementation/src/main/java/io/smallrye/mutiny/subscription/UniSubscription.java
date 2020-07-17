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

import io.smallrye.mutiny.Uni;

/**
 * A {@link Subscription} for the {@link Uni} type.
 * <p>
 * The main different with the Reactive Streams Subscription is about the <em>request</em> protocol. Uni does not use
 * request and triggers the computation at subscription time.
 */
public interface UniSubscription extends Subscription, Cancellable {

    /**
     * Requests the {@link Uni} to cancel and clean up resources.
     * If the item is retrieved after cancellation, it is not forwarded to the subscriber.
     * If the cancellation happens after the delivery of the item, this call is ignored.
     * <p>
     * Calling this method, emits the {@code cancellation} event upstream.
     */
    void cancel();

    @Override
    default void request(long n) {
        if (n < 1) {
            throw new IllegalArgumentException("Invalid request");
        }
        // Ignored, on Uni the request happen at subscription time.
    }
}
