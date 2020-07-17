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
package io.smallrye.mutiny.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.MultiOperator;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A {@link Multi} subscribing upstream when a number of subscribers is reached.
 *
 * @param <T> the type of item.
 */
public class MultiConnectAfter<T> extends MultiOperator<T, T> {
    private final int numberOfSubscribers;
    private final AtomicInteger count = new AtomicInteger();
    private final ConnectableMultiConnection connection;

    public MultiConnectAfter(ConnectableMulti<T> upstream,
            int numberOfSubscribers,
            ConnectableMultiConnection connection) {
        super(upstream);
        this.numberOfSubscribers = numberOfSubscribers;
        this.connection = connection;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        if (downstream == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        // TODO Wondering if we can just delay the subscription and not call connect.
        upstream().subscribe().withSubscriber(downstream);
        if (count.incrementAndGet() == numberOfSubscribers) {
            ((ConnectableMulti) upstream()).connect(connection);
        }
    }
}
