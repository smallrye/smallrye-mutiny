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
package tck;

import static org.testng.Assert.assertEquals;
import static tck.Await.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiOnITemTransformToMultiAndMergeTckTest extends AbstractPublisherTck<Long> {

    private ScheduledExecutorService executor;

    @BeforeTest
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
    }

    @AfterTest
    public void shutdown() {
        executor.shutdown();
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    @Test
    public void flatMapStageShouldMapElements() {

        assertEquals(await(Multi.createFrom().items(1, 2, 3)
                .emitOn(executor)
                .onItem().transformToMultiAndMerge(n -> Multi.createFrom().items(n, n, n))
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 1, 1, 2, 2, 2, 3, 3, 3));
    }

    @Test
    public void flatMapStageShouldAllowEmptySubStreams() {
        assertEquals(await(Multi.createFrom().items(Multi.createFrom().empty(), Multi.createFrom().items(1, 2))
                .onItem().transformToMultiAndMerge(Function.identity())
                .collectItems().asList()
                .subscribeAsCompletionStage()), Arrays.asList(1, 2));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapStageShouldHandleExceptions() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = infiniteStream()
                .on().termination((f, c) -> {
                    if (c) {
                        cancelled.complete(null);
                    }
                })
                .onItem().transformToMultiAndMerge(foo -> {
                    throw new QuietRuntimeException("failed");
                })
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapStageShouldPropagateUpstreamExceptions() {
        await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .onItem().transformToMultiAndMerge(x -> Multi.createFrom().item(x))
                .collectItems().asList()
                .subscribeAsCompletionStage());
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void flatMapStageShouldPropagateSubstreamExceptions() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        CompletionStage<List<Object>> result = infiniteStream()
                .on().termination(() -> cancelled.complete(null))
                .onItem().transformToMultiAndMerge(f -> Multi.createFrom().failure(new QuietRuntimeException("failed")))
                .collectItems().asList()
                .subscribeAsCompletionStage();
        await(cancelled);
        await(result);
    }

    @Test
    public void flatMapStageShouldPropagateCancelToSubstreams() {
        CompletableFuture<Void> outerCancelled = new CompletableFuture<>();
        CompletableFuture<Void> innerCancelled = new CompletableFuture<>();
        await(infiniteStream()
                .on().termination(() -> outerCancelled.complete(null))
                .onItem().transformToMultiAndMerge(i -> infiniteStream().on().termination(() -> innerCancelled.complete(null)))
                .transform().byTakingFirstItems(5)
                .collectItems().asList()
                .subscribeAsCompletionStage());

        await(outerCancelled);
        await(innerCancelled);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return Multi.createFrom().items(LongStream.rangeClosed(1, elements).boxed())
                .onItem().transformToMultiAndMerge(x -> Multi.createFrom().item(x));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return Multi.createFrom().<Long> failure(new RuntimeException("failed"))
                .onItem().transformToMultiAndMerge(x -> Multi.createFrom().item(x));
    }
}