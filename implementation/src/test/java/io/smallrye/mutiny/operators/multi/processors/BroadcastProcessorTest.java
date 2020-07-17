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
package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class BroadcastProcessorTest {

    private ExecutorService executor;

    @BeforeMethod
    public void setup() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterMethod
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test
    public void testWithTwoSubscribers() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        MultiAssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("four");
        processor.onComplete();

        subscriber1
                .assertReceived("one", "two", "three", "four")
                .assertCompletedSuccessfully();

        subscriber2
                .assertReceived("four")
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithTwoSubscribersSerialized() {
        SerializedProcessor<String, String> processor = BroadcastProcessor.<String> create().serialized();

        MultiAssertSubscriber<String> subscriber1 = MultiAssertSubscriber.create(10);
        MultiAssertSubscriber<String> subscriber2 = MultiAssertSubscriber.create(10);

        processor.subscribe(subscriber1);

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        processor.subscribe(subscriber2);

        processor.onNext("four");
        processor.onComplete();

        subscriber1
                .assertReceived("one", "two", "three", "four")
                .assertCompletedSuccessfully();

        subscriber2
                .assertReceived("four")
                .assertCompletedSuccessfully();
    }

    @Test
    public void testSubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        MultiAssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onComplete();

        MultiAssertSubscriber<String> subscriber3 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        subscriber1
                .assertReceived("one", "two", "three")
                .assertCompletedSuccessfully();

        subscriber2
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();

        subscriber3
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testSubscriptionAfterFailure() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        MultiAssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onError(new Exception("boom"));

        MultiAssertSubscriber<String> subscriber3 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        subscriber1
                .assertReceived("one", "two", "three")
                .assertHasFailedWith(Exception.class, "boom");

        subscriber2
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(Exception.class, "boom");

        subscriber3
                .assertHasNotReceivedAnyItem()
                .assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testFailureAfterCompletion() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiAssertSubscriber<Integer> subscriber = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();
        processor.onError(new Exception("boom"));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2);
    }

    @Test
    public void testNoITemAfterCancellation() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertReceived("one");

        MultiAssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("two");

        subscriber1.assertReceived("one", "two");
        subscriber2.assertReceived("two");

        subscriber1.cancel();

        processor.onNext("three");

        subscriber1.assertNotTerminated().assertReceived("one", "two");
        subscriber2.assertReceived("two", "three");

        processor.onComplete();

        subscriber2.assertReceived("two", "three").assertCompletedSuccessfully();
        subscriber1.assertNotTerminated().assertReceived("one", "two");

        processor.onNext("four");

        subscriber2.assertReceived("two", "three").assertCompletedSuccessfully();
        subscriber1.assertNotTerminated().assertReceived("one", "two");
    }

    @Test
    public void testResubscription() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertReceived("one");
        subscriber1.cancel();

        processor.onNext("two");

        processor.subscribe(subscriber1);

        processor.onNext("three");
        processor.onComplete();

        subscriber1.assertReceived("one", "three")
                .assertCompletedSuccessfully();

    }

    @Test
    public void testResubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertReceived("one");
        subscriber1.cancel();

        processor.onNext("two");
        processor.onComplete();

        processor.subscribe(subscriber1);

        subscriber1.assertReceived("one")
                .assertCompletedSuccessfully();

    }

    @Test
    public void testResubscriptionAfterFailure() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertReceived("one");
        subscriber1.cancel();

        processor.onNext("two");
        processor.onError(new IOException("boom"));

        processor.subscribe(subscriber1);

        subscriber1.assertReceived("one")
                .assertHasFailedWith(IOException.class, "boom");

    }

    @Test
    public void testUsingTheProcessorWithAnUpstream() {
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();

        MultiAssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));
        MultiAssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        upstream.subscribe(processor);

        s1.assertCompletedSuccessfully()
                .assertReceived(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        s2.assertCompletedSuccessfully()
                .assertReceived(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamAndLateSubscriber() {
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        MultiAssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        s1.assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamAlreadyCompleted() {
        Multi<Integer> upstream = Multi.createFrom().empty();
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        MultiAssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));

        s1.assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamThatHasAlreadyFailed() {
        Multi<Integer> upstream = Multi.createFrom().failure(new Exception("boom"));
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        MultiAssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));
        MultiAssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));
        s1.assertHasFailedWith(Exception.class, "boom");
        s2.assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testWhenSubscriberDoesNotHaveRequestedEnough() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiAssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(10));
        MultiAssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(4));

        for (int i = 0; i < 10; i++) {
            processor.onNext(i);
        }
        processor.onComplete();

        s1.assertCompletedSuccessfully()
                .assertReceived(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        s2.assertHasFailedWith(BackPressureFailure.class, "request");
    }

    @Test
    public void testCrossCancellation() {
        MultiAssertSubscriber<Integer> subscriber1 = MultiAssertSubscriber.create(10);
        MultiAssertSubscriber<Integer> subscriber2 = new MultiAssertSubscriber<Integer>(10) {
            @Override
            public synchronized void onNext(Integer o) {
                super.onNext(o);
                subscriber1.cancel();
            }
        };
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        processor.subscribe().withSubscriber(subscriber2);
        processor.subscribe().withSubscriber(subscriber1);
        processor.onNext(1);
        subscriber2.assertReceived(1);
        subscriber1.assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCrossCancellationOnFailure() {
        MultiAssertSubscriber<Integer> subscriber1 = MultiAssertSubscriber.create(10);
        MultiAssertSubscriber<Integer> subscriber2 = new MultiAssertSubscriber<Integer>(10) {
            @Override
            public synchronized void onError(Throwable failure) {
                super.onError(failure);
                subscriber1.cancel();
            }
        };
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        processor.subscribe().withSubscriber(subscriber2);
        processor.subscribe().withSubscriber(subscriber1);
        processor.onError(new Exception("boom"));
        subscriber2.assertHasFailedWith(Exception.class, "boom");
        subscriber1.assertHasNotReceivedAnyItem().assertNotTerminated();
    }

    @Test(invocationCount = 100)
    public void testCompletionRace() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(1);
        processor.subscribe(subscriber);

        final AtomicInteger count = new AtomicInteger(2);
        Runnable task = () -> {
            if (count.decrementAndGet() != 0) {
                while (count.get() != 0) {
                    // Explicit loop
                }
            }
            processor.onComplete();
        };
        executor.submit(task);
        executor.submit(task);

        subscriber.await(Duration.ofSeconds(5)).assertCompletedSuccessfully();
    }

    @Test(invocationCount = 100)
    public void testCompletionVsSubscriptionRace() throws InterruptedException {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(1);

        CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger count = new AtomicInteger(2);
        Runnable task1 = () -> {
            if (count.decrementAndGet() != 0) {
                while (count.get() != 0) {
                    // Explicit loop
                }
            }
            processor.onComplete();
            latch.countDown();
        };
        Runnable task2 = () -> {
            if (count.decrementAndGet() != 0) {
                while (count.get() != 0) {
                    // Explicit loop
                }
            }
            processor.subscribe(subscriber);
            latch.countDown();
        };
        executor.submit(task1);
        executor.submit(task2);

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testWithTransformToMultiAndMerge() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        processor
                .onItem().transformToMulti(i -> processor).withRequests(10).merge()
                .subscribe().withSubscriber(subscriber);
        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onComplete();

        subscriber.assertReceived(2, 3, 3, 4, 4, 4)
                .assertCompletedSuccessfully();

    }

    @Test
    public void testMakingTicksAHotStream() throws InterruptedException {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .transform().byTakingFirstItems(100);
        BroadcastProcessor<Long> processor = BroadcastProcessor.create();
        ticks.subscribe().withSubscriber(processor);
        Thread.sleep(50); // NOSONAR

        MultiAssertSubscriber<Long> subscriber = processor.subscribe()
                .withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .await(Duration.ofSeconds(10))
                .assertCompletedSuccessfully();

        List<Long> items = subscriber.items();
        assertThat(items).isNotEmpty().doesNotContain(0L, 1L, 2L, 3L, 4L);
    }
}
