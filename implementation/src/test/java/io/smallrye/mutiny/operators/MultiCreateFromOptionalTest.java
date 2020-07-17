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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCreateFromOptionalTest {

    @SuppressWarnings("OptionalAssignedToNull")
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheOptionalCannotBeNull() {
        Multi.createFrom().optional((Optional<String>) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatOptionalSupplierCannotBeNull() {
        Multi.createFrom().optional((Supplier<Optional<String>>) null);
    }

    @Test
    public void testWithAValue() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().optional(Optional.of("hello")).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertReceived("hello");
    }

    @Test
    public void testWithEmpty() {
        MultiAssertSubscriber<String> subscriber = Multi.createFrom().<String> optional(Optional.empty()).subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));
        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithAValueProducedInSupplier() {
        AtomicInteger count = new AtomicInteger();

        Multi<String> multi = Multi.createFrom()
                .optional(() -> Optional.of("hello-" + count.incrementAndGet()));
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertReceived("hello-1");
        subscriber2.assertHasNotReceivedAnyItem().assertNotTerminated().request(20)
                .assertCompletedSuccessfully().assertReceived("hello-2");
    }

    @Test
    public void testWithEmptyProducedInSupplier() {
        Multi<String> multi = Multi.createFrom().optional(Optional::empty);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1));
        MultiAssertSubscriber<String> subscriber2 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber1.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        subscriber2.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testWithExceptionThrownBySupplier() {
        Multi<String> multi = Multi.createFrom().optional(() -> {
            throw new IllegalStateException("boom");
        });
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated().assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @SuppressWarnings("OptionalAssignedToNull")
    @Test
    public void testWithNullReturnedBySupplier() {
        Multi<String> multi = Multi.createFrom().optional(() -> null);
        MultiAssertSubscriber<String> subscriber1 = multi.subscribe().withSubscriber(MultiAssertSubscriber.create());
        subscriber1.assertTerminated();

        assertThat(subscriber1.failures()).hasSize(1)
                .allSatisfy(t -> assertThat(t).isInstanceOf(NullPointerException.class));
    }
}
