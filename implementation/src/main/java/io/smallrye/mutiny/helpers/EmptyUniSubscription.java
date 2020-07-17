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
package io.smallrye.mutiny.helpers;

import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * An implementation of {@link UniSubscription} ignoring all call to {@link #cancel()}.
 * This implementation should be accessed using the {@link #CANCELLED} instance.
 */
public class EmptyUniSubscription implements UniSubscription {

    /**
     * The instance that can be shared.
     * Calling {@link #cancel()} is a no-op.
     */
    public static final UniSubscription CANCELLED = new EmptyUniSubscription();

    private EmptyUniSubscription() {
        // Avoid direct instantiation.
    }

    public static <T> void propagateFailureEvent(UniSubscriber<T> subscriber, Throwable failure) {
        subscriber.onSubscribe(CANCELLED);
        if (failure == null) {
            subscriber.onFailure(new NullPointerException());
        } else {
            subscriber.onFailure(failure);
        }
    }

    @Override
    public void cancel() {
        // Ignored.
    }
}
