package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.queues.Queues;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.test.AssertSubscriber;

public class UnicastProcessorTest {

    @Test
    public void testTheProcessorCanGetOnlyOneSubscriber() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();
        processor.subscribe()
                .withSubscriber(AssertSubscriber.create());
        AssertSubscriber<Integer> second = processor.subscribe()
                .withSubscriber(AssertSubscriber.create());

        second.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, null)
                .assertHasNotCompleted();
    }

    @RepeatedTest(100)
    public void testWithMultithreadedUpstream() {
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

        await().until(() -> subscriber.items().size() == 5 * 1000);
        processor.onComplete();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 1000; j++) {
                assertThat(subscriber.items()).contains(i + "-" + j);
            }
        }

        executor.shutdownNow();
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

        processor.onSubscribe(mock(Subscription.class));
        processor.onNext("a");
        processor.onNext("b");
        processor.onComplete();

        subscriber.assertNotTerminated()
                .assertSubscribed()
                .assertHasNotReceivedAnyItem();
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
                .assertReceived(1, 2)
                // The overflow is only propagated on the next request.
                .request(2)
                .assertHasFailedWith(BackPressureFailure.class, "");
    }

}
