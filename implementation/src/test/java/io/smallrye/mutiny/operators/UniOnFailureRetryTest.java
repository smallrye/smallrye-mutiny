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

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UniOnFailureRetryTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidNumberOfAttempts() {
        Uni.createFrom().nothing().onFailure().retry().atMost(-10);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidNumberOfAttemptsWithZero() {
        Uni.createFrom().nothing().onFailure().retry().atMost(0);
    }

    @Test
    public void testNoRetryOnItem() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().item(1)
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);
        ts.assertItem(1);

    }

    @Test
    public void testWithOneRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(() -> {
            int i = count.getAndIncrement();
            if (i < 1) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);

        ts
                .assertCompletedSuccessfully()
                .assertItem(1);

    }

    @Test
    public void testWithInfiniteRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(() -> {
            int i = count.getAndIncrement();
            if (i < 10) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().indefinitely()
                .subscribe().withSubscriber(ts);

        ts
                .assertCompletedSuccessfully()
                .assertItem(10);
    }

    @Test
    public void testWithMapperFailure() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(1)
                .onItem().invoke(input -> {
                    if (count.incrementAndGet() < 2) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(1);
    }
}
