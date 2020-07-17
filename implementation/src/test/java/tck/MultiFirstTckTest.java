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
package tck;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static tck.Await.await;

import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiFirstTckTest extends AbstractTck {
    @Test
    public void findFirstStageShouldFindTheFirstElement() {
        int res = await(
                Multi.createFrom().items(1, 2, 3)
                        .collectItems().first()
                        .subscribeAsCompletionStage());
        assertEquals(res, 1);
    }

    @Test
    public void findFirstStageShouldFindTheFirstElementInSingleElementStream() {
        int result = await(Multi.createFrom().item(1)
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }

    @Test
    public void findFirstStageShouldReturnEmptyForEmptyStream() {
        assertNull(await(Multi.createFrom().items()
                .collectItems().first().subscribeAsCompletionStage()), null);
    }

    @Test
    public void findFirstStageShouldCancelUpstream() {
        CompletableFuture<Void> cancelled = new CompletableFuture<>();
        int result = await(infiniteStream()
                .onTermination().invoke(() -> cancelled.complete(null))
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
        await(cancelled);
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void findFirstStageShouldPropagateErrors() {
        await(Multi.createFrom().failure(new QuietRuntimeException("failed"))
                .collectItems().first().subscribeAsCompletionStage());
    }

    @Test
    public void findFirstStageShouldBeReusable() {
        int result = await(Multi.createFrom().items(1, 2, 3)
                .collectItems().first().subscribeAsCompletionStage());
        assertEquals(result, 1);
    }
}
