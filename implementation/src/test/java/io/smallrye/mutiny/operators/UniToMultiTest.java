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

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class UniToMultiTest {

    @Test
    public void testFromEmpty() {
        Multi<Void> multi = Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty2() {
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().voidItem());
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty3() {
        Multi<Void> multi = Uni.createFrom().voidItem().toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty4() {
        Multi<String> multi = Uni.createFrom().<String> nullItem().toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty5() {
        Multi<String> multi = Multi.createFrom().uni(Uni.createFrom().nullItem());
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromResult() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom().item(count::incrementAndGet).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromResult2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom().item(count::incrementAndGet));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .<Integer> failure(() -> new IOException("boom-" + count.incrementAndGet()))
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testFromFailure2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet())));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithNoEvents() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Uni.createFrom().<Void> nothing()
                .on().cancellation(() -> called.set(true))
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testWithNoEvents2() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().<Void> nothing()
                .on().cancellation(() -> called.set(true)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .completionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .await()
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom()
                .completionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .await()
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture() {
        Multi<Integer> multi = Uni.createFrom()
                .completionStage(() -> CompletableFuture.<Integer> supplyAsync(() -> null)).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture2() {
        Multi<Integer> multi = Multi.createFrom()
                .uni(Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> null)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }
}
