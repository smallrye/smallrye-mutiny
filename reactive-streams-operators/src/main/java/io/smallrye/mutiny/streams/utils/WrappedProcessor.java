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

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.StrictMultiSubscriber;

/**
 * Processor wrapping a publisher and subscriber, and connect them
 */
public class WrappedProcessor<T> implements Processor<T, T> {
    private final Subscriber<T> subscriber;
    private final Publisher<T> publisher;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    public WrappedProcessor(Subscriber<T> subscriber, Publisher<T> publisher) {
        this.subscriber = subscriber;
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        publisher.subscribe(new StrictMultiSubscriber<>(subscriber));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Objects.requireNonNull(subscription);
        if (!subscribed.compareAndSet(false, true)) {
            subscription.cancel();
        } else {
            subscriber.onSubscribe(subscription);
        }
    }

    @Override
    public void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(Objects.requireNonNull(throwable));
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
