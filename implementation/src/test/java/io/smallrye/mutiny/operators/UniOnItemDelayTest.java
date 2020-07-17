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
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnItemDelayTest {

    private ScheduledExecutorService executor;

    private Uni<Void> delayed;

    @BeforeMethod
    public void init() {
        executor = Executors.newScheduledThreadPool(4);
        delayed = Uni.createFrom().voidItem().onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100));
    }

    @AfterMethod
    public void shutdown() {
        executor.shutdown();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNullDuration() {
        Uni.createFrom().item(1).onItem().delayIt().by(null);
    }

    @Test
    public void testDelayOnItemWithDefaultExecutor() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();

        Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .onItem().delayIt()
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);

        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompletedSuccessfully().assertItem(null);
        assertThat(subscriber.getOnItemThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithNegativeDuration() {
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofDays(-1));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWithZeroAsDuration() {
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ZERO);
    }

    @Test
    public void testDelayOnItem() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        delayed.subscribe().withSubscriber(subscriber);
        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isGreaterThanOrEqualTo(100);
        subscriber.assertCompletedSuccessfully().assertItem(null);
        assertThat(subscriber.getOnItemThreadName()).isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    public void testThatDelayDoNotImpactFailures() {
        long begin = System.currentTimeMillis();
        UniAssertSubscriber<Void> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().<Void> failure(new Exception("boom")).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.await();
        long end = System.currentTimeMillis();
        assertThat(end - begin).isLessThan(100);
        subscriber.assertCompletedWithFailure().assertFailure(Exception.class, "boom");
    }

    @Test
    public void testThatNothingIsSubmittedOnImmediateCancellation() {
        AtomicBoolean called = new AtomicBoolean();
        executor.shutdown();
        executor = new ScheduledThreadPoolExecutor(4) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                called.set(true);
                return super.schedule(command, delay, unit);
            }
        };

        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>(true);
        Uni.createFrom().item(1).onItem().delayIt().onExecutor(executor).by(Duration.ofMillis(100)).subscribe()
                .withSubscriber(subscriber);
        subscriber.assertNotCompleted();
        assertThat(called).isFalse();
    }

    @Test
    public void testRejectedScheduling() {
        executor.shutdown();
        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.assertCompletedWithFailure().assertFailure(RejectedExecutionException.class, "");
    }

    @Test
    public void testCancellationHappeningDuringTheWaitingTime() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
        executor.shutdown();

        executor = new ScheduledThreadPoolExecutor(4) {
            @Override
            public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
                ScheduledFuture<?> schedule = super.schedule(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    command.run();

                }, delay, unit);
                future.set(schedule);
                return schedule;
            }
        };

        UniAssertSubscriber<Integer> subscriber = new UniAssertSubscriber<>();
        Uni.createFrom().item(1).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(100)).subscribe().withSubscriber(subscriber);
        subscriber.cancel();
        latch.countDown();

        await().until(() -> future.get() != null && future.get().isCancelled());
        subscriber.assertNotCompleted();
    }

    @Test
    public void testWithMultipleDelays() {
        AtomicLong counter = new AtomicLong();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(50))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);

        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(200))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(400))
                .subscribe().with(v -> counter.incrementAndGet(), failure::set);
        Uni.createFrom().item((Object) null).onItem().delayIt()
                .onExecutor(executor)
                .by(Duration.ofMillis(800)).subscribe().with(v -> counter.incrementAndGet(), failure::set);

        await().until(() -> counter.intValue() == 4);
        assertThat(failure.get()).isNull();

    }
}
