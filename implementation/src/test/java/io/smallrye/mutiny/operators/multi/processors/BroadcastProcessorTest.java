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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class BroadcastProcessorTest {

    private ExecutorService executor;

    @BeforeEach
    public void setup() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void cleanup() {
        executor.shutdownNow();
    }

    @Test
    public void testWithTwoSubscribers() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("four");
        processor.onComplete();

        subscriber1
                .assertItems("one", "two", "three", "four")
                .assertCompleted();

        subscriber2
                .assertItems("four")
                .assertCompleted();
    }

    @Test
    public void testWithTwoSubscribersSerialized() {
        SerializedProcessor<String, String> processor = BroadcastProcessor.<String> create().serialized();

        AssertSubscriber<String> subscriber1 = AssertSubscriber.create(10);
        AssertSubscriber<String> subscriber2 = AssertSubscriber.create(10);

        processor.subscribe(subscriber1);

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        processor.subscribe(subscriber2);

        processor.onNext("four");
        processor.onComplete();

        subscriber1
                .assertItems("one", "two", "three", "four")
                .assertCompleted();

        subscriber2
                .assertItems("four")
                .assertCompleted();
    }

    @Test
    public void testSubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onComplete();

        AssertSubscriber<String> subscriber3 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        subscriber1
                .assertItems("one", "two", "three")
                .assertCompleted();

        subscriber2
                .assertHasNotReceivedAnyItem()
                .assertCompleted();

        subscriber3
                .assertHasNotReceivedAnyItem()
                .assertCompleted();
    }

    @Test
    public void testSubscriptionAfterFailure() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onError(new Exception("boom"));

        AssertSubscriber<String> subscriber3 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        subscriber1
                .assertItems("one", "two", "three")
                .assertFailedWith(Exception.class, "boom");

        subscriber2
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(Exception.class, "boom");

        subscriber3
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testFailureAfterCompletion() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> subscriber = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();
        processor.onError(new Exception("boom"));

        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testNoITemAfterCancellation() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertItems("one");

        AssertSubscriber<String> subscriber2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("two");

        subscriber1.assertItems("one", "two");
        subscriber2.assertItems("two");

        subscriber1.cancel();

        processor.onNext("three");

        subscriber1.assertNotTerminated().assertItems("one", "two");
        subscriber2.assertItems("two", "three");

        processor.onComplete();

        subscriber2.assertItems("two", "three").assertCompleted();
        subscriber1.assertNotTerminated().assertItems("one", "two");

        processor.onNext("four");

        subscriber2.assertItems("two", "three").assertCompleted();
        subscriber1.assertNotTerminated().assertItems("one", "two");
    }

    @Test
    public void testResubscription() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertItems("one");
        subscriber1.cancel();

        processor.onNext("two");

        processor.subscribe(subscriber1);

        processor.onNext("three");
        processor.onComplete();

        subscriber1.assertItems("one", "three")
                .assertCompleted();

    }

    @Test
    public void testResubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertItems("one");
        subscriber1.cancel();

        processor.onNext("two");
        processor.onComplete();

        processor.subscribe(subscriber1);

        subscriber1.assertItems("one")
                .assertCompleted();

    }

    @Test
    public void testResubscriptionAfterFailure() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        AssertSubscriber<String> subscriber1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertItems("one");
        subscriber1.cancel();

        processor.onNext("two");
        processor.onError(new IOException("boom"));

        processor.subscribe(subscriber1);

        subscriber1.assertItems("one")
                .assertFailedWith(IOException.class, "boom");

    }

    @Test
    public void testUsingTheProcessorWithAnUpstream() {
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();

        AssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        upstream.subscribe(processor);

        s1.assertCompleted()
                .assertItems(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        s2.assertCompleted()
                .assertItems(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamAndLateSubscriber() {
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        AssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        s1.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamAlreadyCompleted() {
        Multi<Integer> upstream = Multi.createFrom().empty();
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        AssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        s1.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testUsingTheProcessorWithAnUpstreamThatHasAlreadyFailed() {
        Multi<Integer> upstream = Multi.createFrom().failure(new Exception("boom"));
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        upstream.subscribe(processor);
        AssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        s1.assertFailedWith(Exception.class, "boom");
        s2.assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testWhenSubscriberDoesNotHaveRequestedEnough() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> s1 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> s2 = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(4));

        for (int i = 0; i < 10; i++) {
            processor.onNext(i);
        }
        processor.onComplete();

        s1.assertCompleted()
                .assertItems(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        s2.assertFailedWith(BackPressureFailure.class, "request");
    }

    @Test
    public void testCrossCancellation() {
        AssertSubscriber<Integer> subscriber1 = AssertSubscriber.create(10);
        AssertSubscriber<Integer> subscriber2 = new AssertSubscriber<Integer>(10) {
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
        subscriber2.assertItems(1);
        subscriber1.assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCrossCancellationOnFailure() {
        AssertSubscriber<Integer> subscriber1 = AssertSubscriber.create(10);
        AssertSubscriber<Integer> subscriber2 = new AssertSubscriber<Integer>(10) {
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
        subscriber2.assertFailedWith(Exception.class, "boom");
        subscriber1.assertHasNotReceivedAnyItem().assertNotTerminated();
    }

    @RepeatedTest(100)
    public void testCompletionRace() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Object> subscriber = AssertSubscriber.create(1);
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

        subscriber.awaitCompletion(Duration.ofSeconds(5)).assertCompleted();
    }

    @RepeatedTest(100)
    public void testCompletionVsSubscriptionRace() throws InterruptedException {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Object> subscriber = AssertSubscriber.create(1);

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
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        processor
                .onItem().transformToMulti(i -> processor).withRequests(10).merge()
                .subscribe().withSubscriber(subscriber);
        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onComplete();

        subscriber.assertItems(2, 3, 3, 4, 4, 4)
                .assertCompleted();

    }

    @Test
    public void testMakingTicksAHotStream() throws InterruptedException {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofMillis(1))
                .select().first(100);
        BroadcastProcessor<Long> processor = BroadcastProcessor.create();
        ticks.subscribe().withSubscriber(processor);
        Thread.sleep(50); // NOSONAR

        AssertSubscriber<Long> subscriber = processor.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion(Duration.ofSeconds(10))
                .assertCompleted();

        List<Long> items = subscriber.getItems();
        assertThat(items).isNotEmpty().doesNotContain(0L, 1L, 2L, 3L, 4L);
    }
}
