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

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.CouplingProcessor;

/**
 * Implementation of the {@link Stage.Coupled} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CoupledStageFactory implements ProcessingStageFactory<Stage.Coupled> {
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Coupled stage) {
        Graph source = Objects.requireNonNull(stage.getPublisher());
        Graph sink = Objects.requireNonNull(stage.getSubscriber());

        Publisher<O> publisher = engine.buildPublisher(source);
        SubscriberWithCompletionStage<I, ?> subscriber = engine.buildSubscriber(sink);

        return upstream -> Multi.createFrom().publisher(
                new CouplingProcessor<>(upstream, subscriber.getSubscriber(), publisher));
    }
}
