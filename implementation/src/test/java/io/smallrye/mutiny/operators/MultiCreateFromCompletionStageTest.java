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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromCompletionStageTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheCompletionStageCannotBeNull() {
        Multi.createFrom().completionStage((CompletionStage<String>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheCompletionStageSupplierCannotBeNull() {
        Multi.createFrom().completionStage((Supplier<CompletionStage<String>>) null);
    }

    @Test
    public void testWithAValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.completedFuture("hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithAsyncValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.supplyAsync(() -> "hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.<String> completedFuture(null)).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithAsyncCompletionWithNull() {
        AtomicBoolean called = new AtomicBoolean();
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .completionStage(CompletableFuture.runAsync(() -> called.set(true))).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.await().assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi = Multi.createFrom()
                .completionStage(() -> CompletableFuture.completedFuture("hello-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNotReceivedAnyItem().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> CompletableFuture.completedFuture(null));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        subscriber2.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().completionStage(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }

    @Test
    public void testCancellation() {
        AtomicBoolean cancelled = new AtomicBoolean();
        CompletableFuture<Integer> never = new CompletableFuture<Integer>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled.set(true);
                return super.cancel(mayInterruptIfRunning);
            }
        };

        Multi<Integer> multi = Multi.createFrom().completionStage(never);
        MultiAssertSubscriber<Integer> subscriber = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertNotTerminated()
                .cancel()
                .assertNotTerminated();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testWithException() {
        MultiAssertSubscriber<String> ts = MultiAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        ts.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithRuntimeException() {
        MultiAssertSubscriber<String> ts = MultiAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IllegalArgumentException("boom"));
        ts.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithExceptionThrownByAStage() {
        MultiAssertSubscriber<String> ts = MultiAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Multi.createFrom().completionStage(() -> cs
                .thenApply(String::toUpperCase)
                .<String> thenApply(s -> {
                    throw new IllegalStateException("boom");
                })).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("bonjour");
        ts.assertHasFailedWith(IllegalStateException.class, "boom");
    }
}
