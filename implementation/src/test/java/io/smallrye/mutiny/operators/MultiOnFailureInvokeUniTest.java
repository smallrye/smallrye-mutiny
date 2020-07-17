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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnFailureInvokeUniTest {

    public static final IOException BOOM = new IOException("boom");
    private final Multi<Integer> numbers = Multi.createFrom().items(1, 2);
    private final Multi<Integer> failed = Multi.createBy().concatenating()
            .streams(numbers, Multi.createFrom().failure(BOOM));
    private final Uni<Void> sub = Uni.createFrom().nullItem();

    @Test
    public void testInvokeUniOnItem() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        MultiAssertSubscriber<Integer> subscriber = numbers.onFailure().invokeUni(i -> {
            failure.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2);
        assertThat(twoGotCalled).hasValue(0);
        assertThat(failure).hasValue(null);
    }

    @Test
    public void testInvokeUniOnFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger twoGotCalled = new AtomicInteger();

        MultiAssertSubscriber<Integer> subscriber = failed.onFailure().invokeUni(i -> {
            failure.set(i);
            return sub.onItem().invoke(c -> twoGotCalled.incrementAndGet());
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber.assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2);
        assertThat(twoGotCalled).hasValue(1);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testFailureInAsyncCallback() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        MultiAssertSubscriber<Integer> subscriber = failed.onFailure().invokeUni(i -> {
            failure.set(i);
            throw new RuntimeException("kaboom");
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "kaboom")
                .assertReceived(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testNullReturnedByAsyncCallback() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        MultiAssertSubscriber<Integer> subscriber = failed.onFailure().invokeUni(i -> {
            failure.set(i);
            return null;
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "null")
                .assertReceived(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testInvokeUniWithSubFailure() {
        AtomicReference<Throwable> failure = new AtomicReference<>();

        MultiAssertSubscriber<Integer> subscriber = failed.onFailure().invokeUni(i -> {
            failure.set(i);
            return Uni.createFrom().failure(new IllegalStateException("d'oh"));
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "d'oh")
                .assertReceived(1, 2);
        assertThat(failure).hasValue(BOOM);
    }

    @Test
    public void testCancellationBeforeActionCompletes() {
        AtomicBoolean terminated = new AtomicBoolean();
        Uni<Object> uni = Uni.createFrom().emitter(e -> e.onTermination(() -> terminated.set(true)));

        MultiAssertSubscriber<Integer> subscriber = failed.onFailure().invokeUni(i -> uni)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        subscriber.cancel();
        //noinspection ConstantConditions
        assertThat(terminated).isTrue();
    }
}