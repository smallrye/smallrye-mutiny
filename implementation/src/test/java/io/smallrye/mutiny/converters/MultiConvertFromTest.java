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
package io.smallrye.mutiny.converters;

import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.BuiltinConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromCompletionStageWithValue() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);

        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .completionStage(valued)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromCompletionStageWithEmpty() {
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);

        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), empty)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        Multi.createFrom().completionStage(empty);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromCompletionStageWithException() {
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(BuiltinConverters.fromCompletionStage(), boom)
                .subscribe()
                .withSubscriber(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(Exception.class, "boom");
    }
}
