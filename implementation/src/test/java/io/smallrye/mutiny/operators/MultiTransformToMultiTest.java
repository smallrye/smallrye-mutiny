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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiTransformToMultiTest {

    @Test
    public void testMapShortcut() {
        Multi.createFrom().items(1, 2)
                .map(i -> i + 1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(2))
                .assertCompletedSuccessfully()
                .assertReceived(2, 3);
    }

    @Test
    public void testConcatMapShortcut() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .concatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testConcatMapShortcutWithEmpty() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .concatMap(i -> Multi.createFrom().<Integer> empty())
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceMultiDeprecated() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().produceMulti(i -> Multi.createFrom().items(i, i)).concatenate()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProducePublisherDeprecated() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().producePublisher(i -> Multi.createFrom().items(i, i)).concatenate()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfItems() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem()
                .transformToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfItemsAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem().transformToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();

        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isEqualTo(current + 1);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfItemsAndFailuresAndFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .onItem().transformToMulti(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000 || i == 100_000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        })))
                .collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(CompositeException.class, "boom");

        assertThat(subscriber.items().size()).isEqualTo(100_000 - 2);
        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test(timeOut = 60000)
    public void testConcatMapWithLotsOfItemsAndFailuresWithoutFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 100_001)
                .concatMap(
                        i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                            if (i == 99000) {
                                throw new IllegalArgumentException("boom");
                            } else {
                                return i;
                            }
                        })))
                .subscribe(subscriber);

        subscriber
                .await()
                .assertHasFailedWith(IllegalArgumentException.class, "boom");

        assertThat(subscriber.items().size()).isEqualTo(99000 - 1);
        int current = 0;
        for (int next : subscriber.items()) {
            assertThat(next).isGreaterThan(current);
            current = next;
        }
    }

    @Test
    public void testConcatMapWithDelayOfFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMulti(i -> Multi.createFrom().items(i, i)).collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testTransformToMultiWithConcatenationAndFailuresAndDelay() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMulti(i -> {
                    if (i == 2) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().items(i, i);
                    }
                }).collectFailures().concatenate()
                .subscribe(subscriber);

        subscriber
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 1, 3, 3);
    }

    @Test
    public void testTransformToMultiAndMergeUsingMultiFlatten() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMulti(i -> {
                    Multi<Integer> m = Multi.createFrom()
                            .completionStage(() -> CompletableFuture.supplyAsync(() -> i));
                    return Multi.createBy().merging().streams(m, m);
                }).merge()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).containsExactlyInAnyOrder(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testTransformToMultiAndConcatenateUsingMultiFlatten() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMulti(i -> {
                    Multi<Integer> m = Multi.createFrom()
                            .completionStage(() -> CompletableFuture.supplyAsync(() -> i));
                    return Multi.createBy().merging().streams(m, m);
                }).concatenate()
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).containsExactly(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testTransformToMultiAndMerge() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMultiAndMerge(i -> {
                    Multi<Integer> m = Multi.createFrom()
                            .completionStage(() -> CompletableFuture.supplyAsync(() -> i));
                    return Multi.createBy().merging().streams(m, m);
                })
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).containsExactlyInAnyOrder(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testTransformToMultiAndConcatenate() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .onItem().transformToMultiAndConcatenate(i -> {
                    Multi<Integer> m = Multi.createFrom()
                            .completionStage(() -> CompletableFuture.supplyAsync(() -> i));
                    return Multi.createBy().merging().streams(m, m);
                })
                .subscribe(subscriber);

        subscriber
                .await()
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).containsExactlyInAnyOrder(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testFlatMapShortcut() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 4)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber
                .await()
                .assertReceived(1, 1, 2, 2, 3, 3).assertCompletedSuccessfully();
    }

    @Test
    public void testThatFlatMapIsNotCalledOnUpstreamFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .flatMap(i -> {
                    count.incrementAndGet();
                    return Multi.createFrom().item(i);
                })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testThatFlatMapIsOnlyCallOnItems() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().<Integer> empty()
                .flatMap(i -> {
                    count.incrementAndGet();
                    return Multi.createFrom().item(i);
                })
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
        assertThat(count).hasValue(0);
    }

    @Test
    public void testRegularFlatMapWithRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        Multi.createFrom().range(1, 4)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();

        subscriber.request(2)
                .run(() -> assertThat(subscriber.items()).hasSize(2))
                .request(2)
                .run(() -> assertThat(subscriber.items()).hasSize(4))
                .request(10)
                .run(() -> assertThat(subscriber.items()).hasSize(6))
                .assertCompletedSuccessfully();
        assertThat(subscriber.items()).contains(1, 1, 2, 2, 3, 3);
    }

    @Test
    public void testFlatMapWithMapperThrowingException() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> {
                    throw new IllegalArgumentException("boom");
                })
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testFlatMapWithMapperReturningNull() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> null)
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testFlatMapWithMapperReturningNullInAMulti() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> Multi.createFrom().item(null))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IllegalArgumentException.class, "supplier");
    }

    @Test
    public void testFlatMapWithMapperProducingFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        AtomicInteger count = new AtomicInteger();
        Multi.createFrom().range(1, 4)
                .<Integer> flatMap(i -> Multi.createFrom().failure(new IOException("boom")))
                .subscribe(subscriber);

        subscriber.assertHasFailedWith(IOException.class, "boom");
        assertThat(count).hasValue(0);
    }

    @Test
    public void testFlatMapWithABitMoreResults() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 2001)
                .flatMap(i -> Multi.createFrom().items(i, i))
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(4000);
    }

    @Test
    public void testTransformToMultiWithConcurrency() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().transformToMulti(i -> Multi.createFrom().items(i, i)).merge(25)
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(20000);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testTransformToMultiWithInvalidConcurrency() {
        Multi.createFrom().range(1, 10001)
                .onItem().transformToMulti(i -> Multi.createFrom().items(i, i))
                .merge(-1);
    }

    @Test
    public void testTransformToMultiWithConcurrencyAndAsyncEmission() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 10001)
                .onItem().transformToMulti(i -> Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                .withRequests(20)
                .merge(25)
                .subscribe(subscriber);

        subscriber.await().assertCompletedSuccessfully();
        assertThat(subscriber.items()).hasSize(10000);
    }

    @Test
    public void testTransformToMultiWithFailurePropagation() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 5)
                .onItem().transformToMulti(i -> {
                    if (i % 2 == 0) {
                        return Multi.createFrom().failure(new IOException("boom"));
                    } else {
                        return Multi.createFrom().item(i);
                    }
                })
                .collectFailures()
                .merge()
                .subscribe(subscriber);

        subscriber.assertReceived(1, 3).assertHasFailedWith(CompositeException.class, "boom");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceCompletionStageDeprecated() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceCompletionStage(i -> CompletableFuture.supplyAsync(() -> i + 1))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(3).contains(2, 3, 4);
    }

    @Test
    public void testProduceCompletionStageAlternative() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(3).contains(2, 3, 4);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceIterableDeprecated() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceIterable(i -> Arrays.asList(i, i + 1))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).hasSize(6).containsExactlyInAnyOrder(1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testTransformToIterable() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().transformToIterable(i -> Arrays.asList(i, i + 1))
                .collectItems().asList().await().indefinitely();
        assertThat(list).hasSize(6).containsExactlyInAnyOrder(1, 2, 2, 3, 3, 4);
    }

    @Test
    public void testThatUpstreamFailureCancelledInnersAndIsPropagated() {
        UnicastProcessor<Integer> processor1 = UnicastProcessor.create();
        UnicastProcessor<Integer> processor2 = UnicastProcessor.create();

        MultiAssertSubscriber<Integer> subscriber = processor1
                .flatMap(x -> processor2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        processor1.onNext(1);
        assertTrue(processor2.hasSubscriber());
        processor1.onError(new IOException("boom"));
        assertFalse(processor2.hasSubscriber());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatUpstreamIsCancelledWhenInnerFails() {
        UnicastProcessor<Integer> processor1 = UnicastProcessor.create();
        UnicastProcessor<Integer> processor2 = UnicastProcessor.create();

        MultiAssertSubscriber<Integer> subscriber = processor1
                .flatMap(x -> processor2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));

        processor1.onNext(1);
        assertTrue(processor2.hasSubscriber());
        processor2.onError(new IOException("boom"));
        assertFalse(processor1.hasSubscriber());
        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

}
