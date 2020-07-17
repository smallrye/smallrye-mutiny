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
import static org.testng.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.tck.spi.QuietRuntimeException;
import org.junit.Test;

public class CoupledStageFactoryTest {

    private <T> PublisherBuilder<T> idlePublisher() {
        return ReactiveStreams.fromCompletionStage(new CompletableFuture<>());
    }

    @Test
    public void testDownstreamCancellation() {
        CompletableFuture<Void> publisherCancelled = new CompletableFuture<>();
        CompletableFuture<Void> downstreamCompleted = new CompletableFuture<>();

        idlePublisher()
                .via(
                        ReactiveStreams.coupled(ReactiveStreams.builder().cancel().build(),
                                idlePublisher()
                                        .onTerminate(() -> {
                                            publisherCancelled.complete(null);
                                        }).buildRs()))
                .onComplete(() -> downstreamCompleted.complete(null))
                .ignore()
                .run();
        await(publisherCancelled);
        await(downstreamCompleted);
        assertThat(publisherCancelled.isDone()).isTrue();
    }

    @Test
    public void coupledStageShouldCancelAndCompleteUpstreamWhenPublisherFails() {
        CompletableFuture<Throwable> subscriberFailed = new CompletableFuture<>();
        CompletableFuture<Void> upstreamCancelled = new CompletableFuture<>();

        idlePublisher()
                .onTerminate(() -> upstreamCancelled.complete(null))
                .via(
                        ReactiveStreams.coupled(
                                ReactiveStreams.builder().onError(subscriberFailed::complete).ignore(),
                                ReactiveStreams.failed(new QuietRuntimeException("failed"))))
                .ignore()
                .run();

        assertTrue(await(subscriberFailed) instanceof QuietRuntimeException);
        await(upstreamCancelled);
    }

    @Test
    public void coupledStageShouldCancelAndFailDownstreamWhenUpstreamFails() {
        CompletableFuture<Void> publisherCancelled = new CompletableFuture<>();
        CompletableFuture<Throwable> downstreamFailed = new CompletableFuture<>();

        ReactiveStreams.<Integer> failed(new QuietRuntimeException("failed"))
                .via(
                        ReactiveStreams.coupled(ReactiveStreams.builder().ignore(),
                                idlePublisher().onTerminate(() -> publisherCancelled.complete(null))))
                .onError(downstreamFailed::complete)
                .ignore()
                .run();

        await(publisherCancelled);
        assertTrue(await(downstreamFailed) instanceof QuietRuntimeException);
    }

    @Test
    public void testUpstreamCancellation() {
        CompletableFuture<Void> subscriberCompleted = new CompletableFuture<>();
        CompletableFuture<Void> upstreamCancelled = new CompletableFuture<>();
        idlePublisher()
                .onTerminate(() -> {
                    upstreamCancelled.complete(null);
                })
                .via(ReactiveStreams.coupled(ReactiveStreams.builder()
                        .onComplete(() -> {
                            subscriberCompleted.complete(null);
                        })
                        .ignore(), ReactiveStreams.empty()))
                .ignore().run();
        await(subscriberCompleted);
        await(upstreamCancelled);
        assertThat(subscriberCompleted.isDone()).isTrue();
    }

    /**
     * Wait for the given future to complete and return its value, using the configured timeout.
     */
    private <T> T await(CompletionStage<T> future) {
        try {
            return future.toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("Future timed out after " + 500 + "ms", e);
        }
    }
}
