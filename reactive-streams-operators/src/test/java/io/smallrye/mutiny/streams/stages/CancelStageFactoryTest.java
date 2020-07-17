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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.junit.Test;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.streams.operators.TerminalStage;

/**
 * Checks the behavior of {@link CancelStageFactory}
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CancelStageFactoryTest extends StageTestBase {

    private final CancelStageFactory factory = new CancelStageFactory();

    @Test
    public void create() throws ExecutionException, InterruptedException {
        TerminalStage<Long, Void> terminal = factory.create(null, new Stage.Cancel() {
        });
        AtomicBoolean cancelled = new AtomicBoolean();
        List<Long> list = new ArrayList<>();
        Multi<Long> publisher = Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .emitOn(Infrastructure.getDefaultExecutor())
                .onItem().invoke(list::add)
                .on().cancellation(() -> cancelled.set(true));
        CompletionStage<Void> stage = terminal.apply(publisher);
        stage.toCompletableFuture().get();

        await().untilAtomic(cancelled, is(true));
        assertThat(list).isEmpty();
        assertThat(cancelled).isTrue();
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(null, null);
    }

    @Test
    public void testImmediateCancellation() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        List<Integer> received = new CopyOnWriteArrayList<>();
        AtomicBoolean done = new AtomicBoolean();
        ReactiveStreams.of(1, 2, 3, 4, 5, 6, 7, 8)
                .onTerminate(() -> cancelled.complete(null))
                .filter(i -> i < 3)
                .peek(received::add)
                .cancel()
                .run().toCompletableFuture().whenComplete((res, err) -> done.set(true));

        assertThat(done).isTrue();
        assertThat(cancelled).isCompletedWithValue(null);
        assertThat(received).isEmpty();
    }

    @Test
    public void cancelStageShouldCancelTheStage() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        ReactiveStreams.fromPublisher(s -> s.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                // ignored.
            }

            @Override
            public void cancel() {
                cancelled.complete(null);
            }
        })).cancel().run();
        await().until(cancelled::isDone);
        assertThat(cancelled).isCompletedWithValue(null);
    }

}
