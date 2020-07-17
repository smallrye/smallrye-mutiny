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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Consumer;

import io.smallrye.mutiny.subscription.UniEmitter;

public class UniCreateWithEmitter<T> extends AbstractUni<T> {
    private final Consumer<UniEmitter<? super T>> consumer;

    public UniCreateWithEmitter(Consumer<UniEmitter<? super T>> consumer) {
        this.consumer = nonNull(consumer, "consumer");
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        DefaultUniEmitter<? super T> emitter = new DefaultUniEmitter<>(subscriber);
        subscriber.onSubscribe(emitter);

        try {
            consumer.accept(emitter);
        } catch (RuntimeException e) {
            // we use the emitter to be sure that if the failure happens after the first event being fired, it
            // will be dropped.
            emitter.fail(e);
        }
    }
}
