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

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiReceiveItemOnTest {

    private ExecutorService executor;

    @BeforeMethod
    public void init() {
        executor = Executors.newFixedThreadPool(4, new ThreadFactory() {
            AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("test-" + count.incrementAndGet());
                return thread;
            }
        });
    }

    @AfterTest
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatExecutorCannotBeNull() {
        Multi.createFrom().item(1).emitOn(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSubscribeOnExecutorCannotBeNull() {
        Multi.createFrom().item(1).subscribeOn(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatRunSubscriptionOnExecutorCannotBeNull() {
        Multi.createFrom().item(1).runSubscriptionOn(null);
    }

    @Test
    public void testThatItemsAreDispatchedOnTheRightThread() {
        Set<String> itemThread = ConcurrentHashMap.newKeySet();
        Set<String> completionThread = ConcurrentHashMap.newKeySet();
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .on().completion(() -> completionThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .await()
                .assertCompletedSuccessfully();

        await().until(() -> subscriber.items().size() == 4);
        assertThat(itemThread).allSatisfy(s -> assertThat(s).startsWith("test-"));
        assertThat(completionThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testThatFailureAreDispatchedOnExecutor() {
        Set<String> itemThread = new LinkedHashSet<>();
        Set<String> failureThread = new LinkedHashSet<>();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .emitOn(executor)
                .onItem().invoke(i -> itemThread.add(Thread.currentThread().getName()))
                .onFailure().invoke(f -> failureThread.add(Thread.currentThread().getName()))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .await()
                .assertHasFailedWith(IOException.class, "boom");

        assertThat(itemThread).isEmpty();
        assertThat(failureThread).hasSizeGreaterThanOrEqualTo(1).allSatisfy(s -> assertThat(s).startsWith("test-"));
    }

    @Test
    public void testWithImmediate() {
        Multi.createFrom().items(1, 2, 3, 4)
                .emitOn(Runnable::run)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .await()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testWithLargeNumberOfItems() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().range(0, 100_000)
                .emitOn(executor)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertCompletedSuccessfully();

        assertThat(subscriber.items()).hasSize(100_000);
        int current = -1;
        for (Integer i : subscriber.items()) {
            assertThat(i).isGreaterThan(current);
            current = i;
        }
    }

    @Test
    public void testSubscribeOn() {
        Multi.createFrom().items(1, 2, 3, 4)
                .subscribeOn(executor)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .await()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testRunSubscriptionOn() {
        Multi.createFrom().items(1, 2, 3, 4)
                .runSubscriptionOn(executor)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .await()
                .assertReceived(1, 2, 3, 4);
    }

}
