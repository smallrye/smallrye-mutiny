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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniOnItemDelayUntilTest {

    private ScheduledExecutorService executor;

    private Uni<Void> delayed;

    @BeforeMethod
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
        delayed = Uni.createFrom().voidItem().onItem().delayIt()
                .onExecutor(executor)
                .until(x -> Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(10)));
    }

    @AfterMethod
    public void shutdown() {
        executor.shutdown();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullFunction() {
        Uni.createFrom().item(1).onItem().delayIt().until(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testWithFunctionReturningNull() {
        Uni.createFrom().item(1).onItem().delayIt().until(x -> null).await().indefinitely();
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testWithFunctionThrowException() {
        Uni.createFrom().item(1).onItem().delayIt().until(x -> {
            throw new IllegalStateException("boom");
        }).await().indefinitely();
    }

    @Test
    public void testWithUpstreamFailure() {
        AtomicBoolean called = new AtomicBoolean();
        assertThatThrownBy(() -> Uni.createFrom().<Integer> failure(new IllegalStateException("boom"))
                .onItem().delayIt().until(i -> {
                    called.set(true);
                    return Uni.createFrom().nullItem();
                }).await().indefinitely()).hasMessageContaining("boom").isInstanceOf(IllegalStateException.class);
        assertThat(called).isFalse();
    }

    @Test
    public void testNoDelay() {
        int i = Uni.createFrom().item(1).onItem().delayIt().until(x -> Uni.createFrom().nullItem())
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
    }

    @Test
    public void testWithDelay() {
        int i = Uni.createFrom().item(1).onItem().delayIt().until(x -> delayed)
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
    }

    @Test
    public void testWithEmission() {
        AtomicReference<UniEmitter<?>> reference = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .onItem().delayIt().until(i -> Uni.createFrom().emitter(reference::set))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        await().until(() -> reference.get() != null);
        subscriber.assertNoResult();
        reference.get().complete(null);
        subscriber.await().assertItem(1);
    }

    @Test
    public void testWithDelayAndExecutor() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "my-thread"));
        AtomicReference<String> thread = new AtomicReference<>();
        int i = Uni.createFrom().item(1)
                .emitOn(executor)
                .onItem().delayIt().onExecutor(exec).until(x -> Uni.createFrom().nullItem()
                        .onItem().invoke(ignored -> {
                            thread.set(Thread.currentThread().getName());
                        }))
                .await().indefinitely();
        assertThat(i).isEqualTo(1);
        assertThat(thread.get()).isEqualTo("my-thread");
        exec.shutdownNow();
    }

    @Test
    public void testCancellationDuringWaitingTime() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                // It will never emit the item
                .onItem().delayIt().until(i -> Uni.createFrom().nothing())
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertNoResult();
        subscriber.cancel();
    }

    @Test
    public void testCancellationDuringWaitingTimeWithEmissionAfterward() {
        AtomicReference<UniEmitter<?>> reference = new AtomicReference<>();
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().item(1)
                .onItem().delayIt().until(i -> Uni.createFrom().emitter(reference::set))
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        await().until(() -> reference.get() != null);
        subscriber.assertNoResult();
        subscriber.cancel();
        reference.get().complete(null);
        subscriber.assertNoResult();
    }
}
