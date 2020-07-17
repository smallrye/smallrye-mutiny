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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.subscription.SafeSubscriber;

@SuppressWarnings({ "PublisherImplementation", "ReactiveStreamsPublisherImplementation" })
public class CouplingProcessor<I, O> implements Publisher<O> {

    private final SubscriptionObserver<I> controller;
    private final Publisher<O> publisher;

    public CouplingProcessor(Publisher<I> source, Subscriber<I> subscriber, Publisher<O> publisher) {
        Objects.requireNonNull(subscriber);
        controller = new SubscriptionObserver<>(source, subscriber);
        this.publisher = publisher;
        controller.run();
    }

    @Override
    public synchronized void subscribe(Subscriber<? super O> subscriber) {
        Objects.requireNonNull(subscriber);
        SubscriptionObserver<O> observer = new SubscriptionObserver<>(this.publisher, new SafeSubscriber<>(subscriber));
        controller.setObserver(observer);
        observer.setObserver(controller);
        observer.run();
    }

}
