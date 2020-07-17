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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiIgnoreTest {

    @Test
    public void test() {
        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignore()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testAsUni() {
        CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(future.join()).isNull();
    }

    @Test(expectedExceptions = CompletionException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testAsUniWithFailure() {
        CompletableFuture<Void> future = Multi.createFrom().items(1, 2, 3, 4)
                .onItem().transform(i -> {
                    if (i == 3) {
                        throw new RuntimeException("boom");
                    }
                    return i;
                })
                .onItem().ignoreAsUni()
                .subscribeAsCompletionStage();
        assertThat(future.join()).isNull();
    }

    @Test
    public void testWithNever() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom().nothing()
                .onItem().ignore()
                .subscribe().withSubscriber(MultiAssertSubscriber.create(4))
                .assertNotTerminated();

        subscriber.cancel();
    }

    @Test
    public void testAsUniWithNever() {
        CompletableFuture<Void> future = Multi.createFrom().nothing()
                .onItem().ignoreAsUni().subscribeAsCompletionStage();

        assertThat(future).isNotCompleted();
        future.cancel(true);
    }
}
