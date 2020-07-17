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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * Creates and disposes the engine.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class StageTestBase {

    PublisherBuilder<Integer> infiniteStream() {
        return ReactiveStreams.fromIterable(() -> {
            AtomicInteger value = new AtomicInteger();
            return IntStream.generate(value::incrementAndGet).boxed().iterator();
        });
    }

    <T> T awaitCompletion(CompletionStage<T> future) {
        try {
            return future.toCompletableFuture().get(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException var3) {
            throw new RuntimeException(var3);
        } catch (ExecutionException var4) {
            if (var4.getCause() instanceof RuntimeException) {
                throw (RuntimeException) var4.getCause();
            } else {
                throw new RuntimeException(var4.getCause());
            }
        } catch (TimeoutException var5) {
            throw new RuntimeException("Future timed out after 500 ms", var5);
        }
    }
}
