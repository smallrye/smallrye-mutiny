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
import static tck.Await.await;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.LongStream;

import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;

public class MultiConcatTckTest extends AbstractPublisherTck<Long> {
    @Test
    public void concatStageShouldConcatTwoGraphs() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().items(4, 5, 6))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6));
    }

    @Test(expectedExceptions = QuietRuntimeException.class, expectedExceptionsMessageRegExp = "failed")
    public void concatStageShouldPropagateExceptionsFromSecondStage() {
        await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().failure(new QuietRuntimeException("failed"))).collectItems().asList()
                        .subscribeAsCompletionStage());
    }

    @Test
    public void concatStageShouldWorkWithEmptyFirstGraph() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().items(1, 2, 3))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithEmptySecondGraph() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().items(1, 2, 3),
                        Multi.createFrom().empty())
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3));
    }

    @Test
    public void concatStageShouldWorkWithBothGraphsEmpty() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createFrom().empty(),
                        Multi.createFrom().empty())
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Collections.emptyList());
    }

    @Test
    public void concatStageShouldSupportNestedConcats() {
        assertEquals(await(
                Multi.createBy().concatenating().streams(
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(1, 2, 3),
                                Multi.createFrom().items(4, 5, 6)),
                        Multi.createBy().concatenating().streams(
                                Multi.createFrom().items(7, 8, 9),
                                Multi.createFrom().items(10, 11, 12)))
                        .collectItems().asList()
                        .subscribeAsCompletionStage()),
                Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        long toEmitFromFirst = elements / 2;

        return Multi.createBy().concatenating().streams(
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(1, toEmitFromFirst).boxed().iterator()),
                Multi.createFrom().iterable(
                        () -> LongStream.rangeClosed(toEmitFromFirst + 1, elements).boxed().iterator()));
    }
}
