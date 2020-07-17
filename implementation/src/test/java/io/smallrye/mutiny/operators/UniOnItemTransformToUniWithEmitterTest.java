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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

@SuppressWarnings("ConstantConditions")
public class UniOnItemTransformToUniWithEmitterTest {

    @Test
    public void testTransformToUniWithImmediateValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni(
                (v, e) -> e.complete(2))
                .subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceUniWithImmediateValueDeprecated() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni.createFrom().item(1).onItem().<Integer> produceUni(
                (v, e) -> e.complete(2))
                .subscribe().withSubscriber(test);
        test.assertCompletedSuccessfully().assertItem(2).assertNoFailure();
    }

    @Test
    public void testWithImmediateCancellation() {
        UniAssertSubscriber<Integer> test = new UniAssertSubscriber<>(true);
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
        }).subscribe().withSubscriber(test);
        test.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAsyncEmitter() {
        UniAssertSubscriber<Integer> test1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> test2 = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger(2);
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> new Thread(() -> e.complete(count.incrementAndGet())).start());
        uni.subscribe().withSubscriber(test1);
        uni.subscribe().withSubscriber(test2);
        test1.await().assertCompletedSuccessfully().assertNoFailure();
        test2.await().assertCompletedSuccessfully().assertNoFailure();
        assertThat(test1.getItem()).isBetween(3, 4);
        assertThat(test2.getItem()).isBetween(3, 4);
    }

    @Test
    public void testWithAsyncEmitterAndFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> new Thread(() -> e.fail(new IOException("boom"))).start());
        uni.subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IOException.class, "boom");
    }

    @Test
    public void testThatMapperIsNotCalledOnUpstreamFailure() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().failure(new Exception("boom")).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(Exception.class, "boom");
        assertThat(called).isFalse();
    }

    @Test
    public void testWithAMapperThrowingAnException() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedWithFailure().assertFailure(IllegalStateException.class, "boom");
        assertThat(called).isTrue();
    }

    @Test
    public void testWithAMapperThrowingAnExceptionAfterEmittingAValue() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni.createFrom().item(1).onItem().<Integer> transformToUni((v, e) -> {
            called.set(true);
            e.complete(2);
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(test);
        test.await().assertCompletedSuccessfully().assertItem(2).assertNoFailure();
        assertThat(called).isTrue();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheMapperCannotBeNull() {
        Uni.createFrom().item(1).onItem().transformToUni((BiConsumer<Integer, UniEmitter<? super Integer>>) null);
    }

    @Test
    public void testWithCancellationBeforeEmission() {
        UniAssertSubscriber<Integer> test = UniAssertSubscriber.create();
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Uni<Integer> uni = Uni.createFrom().item(1).onItem()
                .transformToUni((v, e) -> future.whenComplete((x, f) -> e.complete(x)));
        uni.subscribe().withSubscriber(test);
        test.cancel();
        test.assertNotCompleted();
    }
}
