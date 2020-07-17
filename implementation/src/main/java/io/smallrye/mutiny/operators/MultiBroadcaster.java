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

import java.time.Duration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.multicast.MultiPublishOp;

public class MultiBroadcaster {

    public static <T> Multi<T> publish(Multi<T> upstream, int numberOfSubscribers, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {

        if (numberOfSubscribers > 0) {
            return createPublishWithSubscribersThreshold(upstream, numberOfSubscribers, cancelWhenNoOneIsListening,
                    delayAfterLastDeparture);
        } else {
            return createPublishImmediate(upstream, cancelWhenNoOneIsListening, delayAfterLastDeparture);
        }
    }

    private static <T> Multi<T> createPublishImmediate(Multi<T> upstream, boolean cancelWhenNoOneIsListening,
            Duration delayAfterLastDeparture) {
        if (cancelWhenNoOneIsListening) {
            if (delayAfterLastDeparture != null) {
                return Infrastructure
                        .onMultiCreation(MultiPublishOp.create(upstream).referenceCount(1, delayAfterLastDeparture));
            } else {
                return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).referenceCount());
            }
        } else {
            return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).connectAfter(1));
        }
    }

    private static <T> Multi<T> createPublishWithSubscribersThreshold(Multi<T> upstream, int numberOfSubscribers,
            boolean cancelWhenNoOneIsListening, Duration delayAfterLastDeparture) {
        if (cancelWhenNoOneIsListening) {
            if (delayAfterLastDeparture != null) {
                return Infrastructure.onMultiCreation(
                        MultiPublishOp.create(upstream).referenceCount(numberOfSubscribers, delayAfterLastDeparture));
            } else {
                // the duration can be `null`, it will be validated if not `null`.
                return Infrastructure
                        .onMultiCreation(MultiPublishOp.create(upstream).referenceCount(numberOfSubscribers, null));
            }
        } else {
            return Infrastructure.onMultiCreation(MultiPublishOp.create(upstream).connectAfter(numberOfSubscribers));
        }
    }

    private MultiBroadcaster() {
        // Avoid direct instantiation.
    }
}
