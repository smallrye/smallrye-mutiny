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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;

public class UniOnItemOrFailureInvokeTest {

    private final Uni<Integer> one = Uni.createFrom().item(1);
    private final Uni<Integer> two = Uni.createFrom().item(2);
    private final Uni<Void> none = Uni.createFrom().nullItem();
    private final Uni<Integer> failed = Uni.createFrom().failure(new IOException("boom"));

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatCallbackMustNotBeNull() {
        Uni.createFrom().item(1).onItemOrFailure().invoke(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSourceMustNotBeNull() {
        new UniOnItemOrFailureMap<>(null, (x, f) -> x);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatCallbackMustNotBeNullWithInvokeUni() {
        Uni.createFrom().item(1).onItemOrFailure().invokeUni(null);
    }

    @Test
    public void testCallbackOnItem() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
        }).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully()
                .assertItem(1);

        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnNullItem() {
        UniAssertSubscriber<Void> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        none.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
        }).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully()
                .assertItem(null);

        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnFailure() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().invoke((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(IOException.class, "boom");

        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnItemThrowingException() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(IllegalStateException.class, "kaboom");
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnItemThrowingExceptionWithInvokeUni() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invoke((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(IllegalStateException.class, "kaboom");
        assertThat(count).hasValue(1);
    }

    @Test
    public void testCallbackOnFailureThrowingException() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().invoke((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
            throw new IllegalStateException("kaboom");
        }).subscribe().withSubscriber(ts);

        ts.assertFailure(CompositeException.class, "kaboom");
        ts.assertFailure(CompositeException.class, "boom");

        assertThat(count).hasValue(1);
    }

    @Test
    public void testWithTwoSubscribers() {
        UniAssertSubscriber<Integer> ts1 = UniAssertSubscriber.create();
        UniAssertSubscriber<Integer> ts2 = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni<Integer> uni = one.onItemOrFailure().invoke((v, f) -> count.incrementAndGet());
        uni.subscribe().withSubscriber(ts1);
        uni.subscribe().withSubscriber(ts2);

        ts1.assertCompletedSuccessfully()
                .assertItem(1);
        ts2.assertCompletedSuccessfully()
                .assertItem(1);
    }

    @Test
    public void testThatCallbackIsCalledOnTheRightExecutorOnItem() {
        UniAssertSubscriber<Integer> ts = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            one
                    .emitOn(executor)
                    .onItemOrFailure().invoke((i, f) -> threadName.set(Thread.currentThread().getName()))
                    .subscribe().withSubscriber(ts);

            ts.await().assertCompletedSuccessfully().assertItem(1);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(ts.getOnItemThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testThatCallbackIsCalledOnTheRightExecutorOnFailure() {
        UniAssertSubscriber<Integer> ts = new UniAssertSubscriber<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> threadName = new AtomicReference<>();
            failed
                    .emitOn(executor)
                    .onItemOrFailure().invoke((i, f) -> {
                        threadName.set(Thread.currentThread().getName());
                        assertThat(i).isNull();
                        assertThat(f).isNotNull();
                    })
                    .onFailure().recoverWithItem(1)
                    .subscribe().withSubscriber(ts);

            ts.await().assertCompletedSuccessfully().assertItem(1);
            assertThat(threadName).isNotNull().doesNotHaveValue("main");
            assertThat(ts.getOnItemThreadName()).isEqualTo(threadName.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testInvokeUniOnItem() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        one.onItemOrFailure().invokeUni((i, f) -> {
            assertThat(f).isNull();
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);
        }).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully()
                .assertItem(1);
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testInvokeUniNullOnItem() {
        UniAssertSubscriber<Void> ts = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        none.onItemOrFailure().invokeUni((i, f) -> {
            assertThat(f).isNull();
            assertThat(i).isNull();
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);
        }).subscribe().withSubscriber(ts);

        ts.assertCompletedSuccessfully().assertItem(null);
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testInvokeUniOnFailure() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicInteger reference = new AtomicInteger();
        AtomicInteger count = new AtomicInteger();
        failed.onItemOrFailure().invokeUni((i, f) -> {
            assertThat(i).isNull();
            assertThat(f).isNotNull().isInstanceOf(IOException.class).hasMessageContaining("boom");
            count.incrementAndGet();
            return two.onItem().invoke(reference::set);

        }).subscribe().withSubscriber(ts);

        ts.assertFailure(IOException.class, "boom");
        assertThat(reference).hasValue(2);
        assertThat(count).hasValue(1);
    }

    @Test
    public void testInvokeUniProducingNullOnItem() {
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> one
                        .onItemOrFailure().invokeUni((s, f) -> null)
                        .await().indefinitely());
    }

    @Test
    public void testInvokeUniProducingNullOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure().invokeUni((s, f) -> null)
                        .await().indefinitely())
                .withMessageContaining("null").withMessageContaining("boom");
    }

    @Test
    public void testInvokeUniFailingOnItem() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> one
                        .onItemOrFailure().invokeUni((s, f) -> {
                            throw new IllegalStateException("boom");
                        })
                        .await().indefinitely());
    }

    @Test
    public void testInvokeUniFailingOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure().invokeUni((s, f) -> {
                            throw new IllegalStateException("d'oh");
                        })
                        .await().indefinitely())
                .withMessageContaining("d'oh").withMessageContaining("boom");
    }

    @Test
    public void testInvokeUniProducingFailureOnItem() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Uni.createFrom().item("hello")
                        .onItemOrFailure()
                        .invokeUni((s, f) -> Uni.createFrom().failure(new IllegalStateException("boom")))
                        .await().indefinitely())
                .withMessageContaining("boom");
    }

    @Test
    public void testInvokeUniProducingFailureOnFailure() {
        assertThatExceptionOfType(CompositeException.class)
                .isThrownBy(() -> failed
                        .onItemOrFailure()
                        .invokeUni((s, f) -> Uni.createFrom().failure(new IllegalStateException("d'oh")))
                        .await().indefinitely())
                .withMessageContaining("d'oh").withMessageContaining("boom");
    }

    @Test
    public void testInvokeUniWithCancellationBeforeEmission() {
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<String> res = new AtomicReference<>();
        Uni<Object> emitter = Uni.createFrom().emitter(e -> e.onTermination(() -> called.set(true)));

        Cancellable cancellable = Uni.createFrom().item("hello")
                .onItemOrFailure().invokeUni((s, f) -> emitter)
                .subscribe().with(res::set);

        cancellable.cancel();
        assertThat(res).hasValue(null);
        //noinspection ConstantConditions
        assertThat(called).isTrue();
    }
}