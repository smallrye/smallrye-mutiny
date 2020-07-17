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
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.operators.TerminalStageFactory;

/**
 * Implementation of {@link Stage.Cancel}. It subscribes and disposes the stream immediately.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CancelStageFactory implements TerminalStageFactory<Stage.Cancel> {

    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.Cancel stage) {
        Objects.requireNonNull(stage);
        return (Multi<I> flow) -> {
            //noinspection SubscriberImplementation
            flow.subscribe(new Subscriber<I>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.cancel();
                }

                @Override
                public void onNext(I in) {
                    // Do nothing.
                }

                @Override
                public void onError(Throwable t) {
                    // Do nothing.
                }

                @Override
                public void onComplete() {
                    // Do nothing.
                }
            });
            return Infrastructure.wrapCompletableFuture(CompletableFuture.completedFuture(null));
        };
    }
}
