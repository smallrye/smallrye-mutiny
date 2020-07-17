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

import io.smallrye.mutiny.Uni;

public class UniCreateFromCompletionStageTest {

    @Test
    public void testThatNullValueAreAccepted() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete(null);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithNonNullValue() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("1");
        ts.assertCompletedSuccessfully().assertItem("1");
    }

    @Test
    public void testWithException() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        ts.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionThrownByAStage() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs
                .thenApply(String::toUpperCase)
                .<String> thenApply(s -> {
                    throw new IllegalStateException("boom");
                })).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("bonjour");
        ts.assertFailure(IllegalStateException.class, "boom");
    }

    @Test
    public void testThatNullValueAreAcceptedWithSupplier() {
        UniAssertSubscriber<Void> ts = UniAssertSubscriber.create();
        Uni.createFrom().<Void> completionStage(() -> CompletableFuture.completedFuture(null)).subscribe()
                .withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testWithNonNullValueWithSupplier() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().complete("1");
        ts.assertCompletedSuccessfully().assertItem("1");
    }

    @Test
    public void testWithExceptionWithSupplier() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        CompletionStage<String> cs = new CompletableFuture<>();
        Uni.createFrom().completionStage(() -> cs).subscribe().withSubscriber(ts);
        cs.toCompletableFuture().completeExceptionally(new IOException("boom"));
        ts.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionInSupplier() {
        UniAssertSubscriber<String> ts = UniAssertSubscriber.create();
        Uni.createFrom().<String> completionStage(() -> {
            throw new NullPointerException("boom");
        }).subscribe().withSubscriber(ts);
        ts.assertFailure(NullPointerException.class, "boom");
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        cs.complete(1);
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscriptionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();

        Uni<Integer> uni = Uni.createFrom().completionStage(() -> {
            called.set(true);
            return cs;
        })
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();

        cs.complete(1);

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmit() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(ts);
        assertThat(called).isFalse();
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfTheStageDoesNotEmitFromSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs)
                .onItem().invoke(i -> called.set(true));

        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(ts);
        assertThat(called).isFalse();
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmission() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> {
                });

        uni.subscribe().withSubscriber(ts);
        ts.cancel();

        cs.complete(1);

        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmissionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs);
        uni.subscribe().withSubscriber(ts);
        ts.cancel();

        cs.complete(1);
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmission() {
        AtomicBoolean called = new AtomicBoolean();
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(cs)
                .onItem().invoke(i -> called.set(true));

        uni.subscribe().withSubscriber(ts);
        cs.complete(1);
        ts.cancel();
        assertThat(called).isTrue();
        ts.assertItem(1);
    }

    @Test
    public void testThatSubscriberCanCancelAfterEmissionWithSupplier() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        CompletableFuture<Integer> cs = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> cs);

        uni.subscribe().withSubscriber(ts);
        cs.complete(1);
        ts.cancel();

        ts.assertItem(1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatCompletionStageCannotBeNull() {
        Uni.createFrom().completionStage((CompletionStage<Void>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatCompletionStageSupplierCannotBeNull() {
        Uni.createFrom().completionStage((Supplier<CompletionStage<Void>>) null);
    }

    @Test
    public void testThatCompletionStageSupplierCannotReturnNull() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> null);

        uni.subscribe().withSubscriber(ts);
        ts.assertFailure(NullPointerException.class, "");
    }

    @Test
    public void testWithSharedState() {
        UniAssertSubscriber<Integer> ts1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> ts2 = UniAssertSubscriber.create();
        AtomicInteger shared = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().completionStage(() -> shared,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        assertThat(shared).hasValue(0);
        uni.subscribe().withSubscriber(ts1);
        assertThat(shared).hasValue(1);
        ts1.assertCompletedSuccessfully().assertItem(1);
        uni.subscribe().withSubscriber(ts2);
        assertThat(shared).hasValue(2);
        ts2.assertCompletedSuccessfully().assertItem(2);
    }

    @Test
    public void testWithSharedStateProducingFailure() {
        UniAssertSubscriber<Integer> ts1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> ts2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> {
            throw new IllegalStateException("boom");
        };

        Uni<Integer> uni = Uni.createFrom().completionStage(boom,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        uni.subscribe().withSubscriber(ts1);
        ts1.assertFailure(IllegalStateException.class, "boom");
        uni.subscribe().withSubscriber(ts2);
        ts2.assertFailure(IllegalStateException.class, "Invalid shared state");
    }

    @Test
    public void testWithSharedStateProducingNull() {
        UniAssertSubscriber<Integer> ts1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> ts2 = UniAssertSubscriber.create();
        Supplier<AtomicInteger> boom = () -> null;

        Uni<Integer> uni = Uni.createFrom().completionStage(boom,
                state -> CompletableFuture.completedFuture(state.incrementAndGet()));

        uni.subscribe().withSubscriber(ts1);
        ts1.assertFailure(NullPointerException.class, "supplier");
        uni.subscribe().withSubscriber(ts2);
        ts2.assertFailure(IllegalStateException.class, "Invalid shared state");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatStateSupplierCannotBeNull() {
        Uni.createFrom().completionStage(null,
                x -> CompletableFuture.completedFuture("x"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionCannotBeNull() {
        Uni.createFrom().completionStage(() -> "hello",
                null);
    }

}
