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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromTimePeriodTest {

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @AfterTest
    public void cleanup() {
        executor.shutdown();
    }

    @Test
    public void testIntervalOfAFewMillis() {
        MultiAssertSubscriber<Long> ts = MultiAssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        ts.items().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(100)).onExecutor(executor).every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(ts);

        await().until(() -> ts.items().size() >= 10);
        ts.cancel();

        ts
                .assertHasNotCompleted()
                .assertHasNotFailed();

        List<Long> list = ts.items();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test
    public void testWithInfraExecutorAndNoDelay() throws InterruptedException {
        MultiAssertSubscriber<Long> ts = MultiAssertSubscriber.create(Long.MAX_VALUE);

        // Add a fake item with the beginning time
        ts.items().add(System.currentTimeMillis());

        // No initial delay, so introduce a fake delay
        Thread.sleep(100);

        Multi.createFrom().ticks()
                .every(Duration.ofMillis(100))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(ts);

        await().until(() -> ts.items().size() >= 10);
        ts.cancel();

        ts
                .assertHasNotCompleted()
                .assertHasNotFailed();

        List<Long> list = ts.items();
        for (int i = 0; i < list.size() - 1; i++) {
            long delta = list.get(i + 1) - list.get(i);
            assertThat(delta).isBetween(20L, 350L);
        }
    }

    @Test(timeOut = 1000)
    public void testBackPressureOverflow() {
        MultiAssertSubscriber<Long> ts = MultiAssertSubscriber.create();

        ts.items().add(System.currentTimeMillis());

        Multi.createFrom().ticks()
                .startingAfter(Duration.ofMillis(50)).onExecutor(executor).every(Duration.ofMillis(50))
                .onItem().transform(l -> System.currentTimeMillis())
                .subscribe().withSubscriber(ts);

        ts
                .request(2) // request only 2
                .await() // wait until failure
                .assertHasFailedWith(BackPressureFailure.class, "lack of requests");
    }

}
