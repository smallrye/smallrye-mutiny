package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;

public class UnicastProcessorTest {

    @Test
    public void testTheProcessorCanGetOnlyOneSubscriber() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        processor.subscribe()
                .withSubscriber(AssertSubscriber.create());
        AssertSubscriber<Integer> second = processor.subscribe()
                .withSubscriber(AssertSubscriber.create());

        second.assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, null);
    }

    @RepeatedTest(100)
    public void testWithMultithreadedUpstream() throws InterruptedException {
        UnicastProcessor<String> processor = UnicastProcessor.create();
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            int t = i;
            Runnable produce = () -> {
                for (int j = 0; j < 1000; j++) {
                    processor.onNext(t + "-" + j);
                }
            };
            executor.submit(produce);
        }

        AssertSubscriber<Object> subscriber = AssertSubscriber.create(Long.MAX_VALUE);
        processor.subscribe(subscriber);

        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The executor shall have terminated");
        }

        processor.onComplete();

        assertThat(subscriber.getItems().size()).isEqualTo(5 * 1000);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                assertThat(subscriber.getItems()).contains(i + "-" + j);
            }
        }
    }

    @Test
    public void testWithImmediateCancellationFromDownstream() {
        UnicastProcessor<String> processor = UnicastProcessor.create();
        AssertSubscriber<String> subscriber = processor
                .subscribe().withSubscriber(new AssertSubscriber<>(50, true));

        processor.onNext("a");
        processor.onNext("b");
        processor.onComplete();

        subscriber.assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();

        processor = UnicastProcessor.create();
        subscriber = processor
                .subscribe().withSubscriber(new AssertSubscriber<>(50, true));

        processor.onNext("a");
        processor.onNext("b");
        processor.onError(new IOException("boom"));

        subscriber.assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testWithImmediateCancellationFromDownstreamWhileWaitingForUpstreamSubscription() {
        UnicastProcessor<String> processor = UnicastProcessor.create();
        AssertSubscriber<String> subscriber = processor
                .subscribe().withSubscriber(new AssertSubscriber<>(50, true));

        processor.onSubscribe(mock(Flow.Subscription.class));
        processor.onNext("a");
        processor.onNext("b");
        processor.onComplete();

        subscriber.assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();
    }

    @RepeatedTest(10)
    public void testWithConcurrentCancellations() {
        AtomicInteger counter = new AtomicInteger();
        Queue<Integer> queue = Queues.<Integer> get(1).get();
        UnicastProcessor<Integer> processor = UnicastProcessor.create(queue, counter::incrementAndGet);

        AssertSubscriber<Integer> sub = processor.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(10);
        CountDownLatch done = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            pool.execute(() -> {
                start.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processor.cancel();
                done.countDown();
            });
        }

        await().until(() -> done.getCount() == 0);
        sub.assertSubscribed().assertHasNotReceivedAnyItem().assertNotTerminated();
        assertThat(counter).hasValue(1);

        pool.shutdownNow();
    }

    @Test
    public void testOverflow() {
        Queue<Integer> queue = Queues.<Integer> get(1).get();
        UnicastProcessor<Integer> processor = UnicastProcessor.create(queue, null);
        AssertSubscriber<Integer> subscriber = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(2));

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);

        subscriber.assertSubscribed()
                .assertItems(1, 2)
                // The overflow is only propagated on the next request.
                .request(2)
                .assertFailedWith(BackPressureFailure.class, "");
    }

}
