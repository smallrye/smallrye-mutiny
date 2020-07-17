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
package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.operators.TerminalStageFactory;
import io.smallrye.mutiny.streams.utils.WrappedSubscriber;

/**
 * Implementation of the {@link Stage.SubscriberStage} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SubscriberStageFactory implements TerminalStageFactory<Stage.SubscriberStage> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.SubscriberStage stage) {
        Subscriber<I> subscriber = (Subscriber<I>) Objects.requireNonNull(stage).getRsSubscriber();
        Objects.requireNonNull(subscriber);
        return (TerminalStage<I, O>) new SubscriberStage<>(subscriber);
    }

    private static class SubscriberStage<I> implements TerminalStage<I, Void> {

        private final Subscriber<I> subscriber;

        SubscriberStage(Subscriber<I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public CompletionStage<Void> apply(Multi<I> source) {
            WrappedSubscriber<I> wrapped = new WrappedSubscriber<>(subscriber);
            source.subscribe().withSubscriber(wrapped);
            return wrapped.future();
        }
    }

}
