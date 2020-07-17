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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromFailureTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureCannotBeNull() {
        Multi.createFrom().failure((Throwable) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFailureSupplierCannotBeNull() {
        Multi.createFrom().failure((Supplier<Throwable>) null);
    }

    @Test
    public void testWithException() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String> failure(new IOException("boom"))
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testWithExceptionSupplier() {
        AtomicInteger count = new AtomicInteger();
        Multi<String> failure = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = failure.subscribe().withSubscriber(MultiAssertSubscriber.create());
        MultiAssertSubscriber<String> subscriber2 = failure.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertHasFailedWith(IOException.class, "boom-1");
        subscriber2.assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().failure(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
