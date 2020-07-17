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

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

@SuppressWarnings("ConstantConditions")
public class UniOnItemTransformToMultiTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTransformToMultiWithNullMapper() {
        Uni<Integer> uni = Uni.createFrom().item(1);
        uni.onItem().transformToMulti(null);
    }

    @Test
    public void testTransformToMultiWithItem() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(i -> Multi.createFrom().range(i, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testTransformToMultiWithItemDeprecated() {
        Uni.createFrom().item(1)
                .onItem().produceMulti(i -> Multi.createFrom().range(i, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testTransformToMultiWithNull() {
        Uni.createFrom().voidItem()
                .onItem().transformToMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testTransformToMultiWithFailure() {
        Uni.createFrom().<Integer> failure(new IOException("boom"))
                .onItem().transformToMulti(x -> Multi.createFrom().range(1, 5))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithExceptionThrownByMapper() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(x -> {
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(IllegalStateException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapper() {
        Uni.createFrom().item(1)
                .onItem().transformToMulti(x -> null)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertHasFailedWith(NullPointerException.class, "")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapperWithCancellationDuringTheUniResolution() {
        final AtomicBoolean called = new AtomicBoolean();

        Uni.createFrom().<Integer> nothing()
                .on().cancellation(() -> called.set(true))
                .onItem().transformToMulti(x -> Multi.createFrom().range(x, 10))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))

                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .assertNotTerminated();
    }

    @Test
    public void testTransformToMultiWithNullReturnedByMapperWithCancellationDuringTheMultiEmissions() {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicBoolean calledUni = new AtomicBoolean();

        Uni.createFrom().item(1)
                .on().cancellation(() -> calledUni.set(true))
                .onItem().transformToMulti(i -> Multi.createFrom().nothing()
                        .on().cancellation(() -> called.set(true)))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .run(() -> assertThat(called).isFalse())
                .cancel()
                .run(() -> assertThat(called).isTrue())
                .run(() -> assertThat(calledUni).isFalse())
                .assertNotTerminated();
    }
}
