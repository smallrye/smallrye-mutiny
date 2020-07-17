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
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.Subscriptions;

/**
 * Wrapped a source publisher and make it cancellable on demand. The cancellation happens if
 * no-one subscribed to the source publisher. This class is required for the
 * {@link io.smallrye.mutiny.streams.stages.ConcatStageFactory} to enforce the reactive
 * streams rules.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@SuppressWarnings("PublisherImplementation")
public class CancellablePublisher<T> implements Publisher<T> {
    private final Publisher<T> source;
    private final AtomicBoolean subscribed = new AtomicBoolean();

    public CancellablePublisher(Publisher<T> delegate) {
        this.source = delegate;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        if (subscribed.compareAndSet(false, true)) {
            source.subscribe(subscriber);
        } else {
            Subscriptions.fail(subscriber, new IllegalStateException("Multicast not supported"));
        }
    }

    public void cancelIfNotSubscribed() {
        if (subscribed.compareAndSet(false, true)) {
            source.subscribe(new Subscriptions.CancelledSubscriber<>());
        }
    }
}
