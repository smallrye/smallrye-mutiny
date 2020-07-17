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
package io.smallrye.mutiny.streams.utils;

import java.util.Objects;

import org.reactivestreams.Subscription;

/**
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class WrappedSubscription implements Subscription {

    private final Subscription subscription;
    private final Runnable cancellationHandler;

    WrappedSubscription(Subscription subscription, Runnable onCancellation) {
        this.subscription = Objects.requireNonNull(subscription);
        this.cancellationHandler = onCancellation;
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        if (cancellationHandler != null) {
            cancellationHandler.run();
        }
        subscription.cancel();
    }
}
