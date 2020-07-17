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

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.CompletionSubscriber;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;

public class DefaultSubscriberWithCompletionStage<T, R> implements SubscriberWithCompletionStage<T, R> {
    private final CompletionSubscriber<T, R> subscriber;

    public DefaultSubscriberWithCompletionStage(Processor<T, T> processor, CompletionStage<R> result) {
        subscriber = CompletionSubscriber.of(processor, result);
    }

    @Override
    public CompletionStage<R> getCompletion() {
        return subscriber.getCompletion();
    }

    @Override
    public Subscriber<T> getSubscriber() {
        return subscriber;
    }
}
