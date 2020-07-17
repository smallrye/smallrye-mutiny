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

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiScanTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatSupplierMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(null, (a, b) -> a);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNull() {
        Multi.createFrom().empty().onItem().scan(() -> 1, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatScannerMustNotBeNullWithoutSupplier() {
        Multi.createFrom().empty().onItem().scan(null);
    }

    @Test
    public void testWithSimplerScanner() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithSimplerScannerWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 2, (a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompletedSuccessfully()
                .assertReceived(2, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> b)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(1, 2, 3, 4, 5)
                .assertNotTerminated();

        subscriber.request(5)
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithAScannerThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan((a, b) -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testWithAScannerThrowingExceptionWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testWithAScannerReturningNullWithSupplier() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(2);

        Multi.createFrom().range(1, 10)
                .onItem().scan(() -> 1, (a, b) -> null)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertReceived(1)
                .assertHasFailedWith(NullPointerException.class, "");
    }
}
