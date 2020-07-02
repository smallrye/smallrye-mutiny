package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class SerializedProcessorTest {

    @Test
    public void testAPI() {
        SerializedProcessor<String, String> processor = new SerializedProcessor<>(UnicastProcessor.create());
        MultiAssertSubscriber<String> subscriber = new MultiAssertSubscriber<>(10);
        processor.subscribe(subscriber);
        processor.onNext("hello");
        processor.onComplete();

        subscriber.await()
                .assertReceived("hello")
                .assertCompletedSuccessfully();
    }

    @Test
    public void testUnicastSerialized() {
        UnicastProcessor<Integer> unicast = UnicastProcessor.create();
        unicast.onNext(1);
        unicast.onComplete();
        SerializedProcessor<Integer, Integer> serialized = unicast.serialized();

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);
        serialized.subscribe(subscriber);
        subscriber.await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testUnicastSerializedWithEmpty() {
        UnicastProcessor<Integer> unicast = UnicastProcessor.create();
        unicast.onComplete();
        SerializedProcessor<Integer, Integer> serialized = unicast.serialized();

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);
        serialized.subscribe(subscriber);
        subscriber.await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testUnicastSerializedWithFailure() {
        UnicastProcessor<Integer> unicast = UnicastProcessor.create();
        unicast.onNext(1);
        unicast.onError(new Exception("boom"));
        SerializedProcessor<Integer, Integer> serialized = unicast.serialized();

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);
        serialized.subscribe(subscriber);
        subscriber.await()
                .assertReceived(1)
                .assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testUnicastSerializedWithNoTerminalEvents() {
        UnicastProcessor<Integer> unicast = UnicastProcessor.create();
        unicast.onNext(1);
        SerializedProcessor<Integer, Integer> serialized = unicast.serialized();

        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(1);
        serialized.subscribe(subscriber);
        subscriber
                .assertReceived(1)
                .assertHasNotCompleted();
    }

    @Test
    public void testWithMultipleItems() {
        Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);
        processor.subscribe(subscriber);

        Multi.createFrom().range(1, 11).subscribe(processor);

        subscriber
                .assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .assertCompletedSuccessfully();

        processor.onNext(11);
        processor.onComplete();
    }

    @Test(invocationCount = 100)
    public void verifyOnNextThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> processor.onNext(1);
        Runnable r2 = () -> processor.onNext(2);

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        await().until(() -> subscriber.items().size() == 2);

        subscriber
                .assertSubscribed()
                .assertNotTerminated();

        List<Integer> items = subscriber.items();
        assertThat(items).hasSize(2).contains(1, 2);
    }

    @Test(invocationCount = 100)
    public void verifyOnErrorThreadSafety() {
        Exception failure = new Exception("boom");
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> processor.onError(failure);
        Runnable r2 = () -> processor.onError(failure);

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber
                .await()
                .assertSubscribed()
                .assertHasFailedWith(Exception.class, "boom");
    }

    @Test(invocationCount = 100)
    public void verifyOnNextOnErrorThreadSafety() {
        Exception failure = new Exception("boom");
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onComplete();
        };
        Runnable r2 = () -> processor.onError(failure);

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        await().until(() -> !subscriber.items().isEmpty() || !subscriber.failures().isEmpty());

        subscriber
                .assertSubscribed()
                .assertTerminated();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1);
        } else {
            assertThat(subscriber.failures()).containsExactly(failure);
        }
    }

    @Test(invocationCount = 100)
    public void verifyOnNextOnCompleteThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onComplete();
        };
        Runnable r2 = processor::onComplete;

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertCompletedSuccessfully();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1);
        }
    }

    @Test(invocationCount = 100)
    public void verifyOnSubscribeOnCompleteThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onComplete();
        };
        Runnable r2 = () -> processor.onSubscribe(new Subscriptions.EmptySubscription());

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertCompletedSuccessfully();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1);
        }
    }

    @Test(invocationCount = 100)
    public void verifyOnSubscribeOnSubscribeThreadSafety() throws InterruptedException {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        CountDownLatch latch = new CountDownLatch(2);
        Runnable r1 = () -> {
            processor.onSubscribe(new Subscriptions.EmptySubscription());
            latch.countDown();
        };
        Runnable r2 = () -> {
            processor.onSubscribe(new Subscriptions.EmptySubscription());
            latch.countDown();
        };

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        latch.await();

        subscriber
                .assertSubscribed();
    }

    @Test(invocationCount = 100)
    public void verifyOnFailureOnCompleteThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onComplete();
        };
        Runnable r2 = () -> processor.onError(new Exception("boom"));

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertTerminated();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1);
        }
    }

    @Test(invocationCount = 100)
    public void verifyOnFailureOnFailureThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> processor.onError(new Exception("boom"));
        Runnable r2 = () -> processor.onError(new Exception("boom"));

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertTerminated()
                .assertHasFailedWith(Exception.class, "boom");
    }

    @Test
    public void testSubscriptionAfterTerminalEvent() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        processor.onComplete();
        Subscription subscription = mock(Subscription.class);
        processor.onSubscribe(subscription);
        verify(subscription).cancel();
    }

    @Test(invocationCount = 100)
    public void testRaceBetweenOnNextAndOnComplete() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onNext(2);
            processor.onNext(3);
            processor.onNext(4);
            processor.onNext(5);
        };
        Runnable r2 = processor::onComplete;

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertCompletedSuccessfully();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).contains(1);
        }

    }

    @Test(invocationCount = 100)
    public void testRaceBetweenOnNextAndOnSubscribe() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onNext(2);
            processor.onNext(3);
            processor.onNext(4);
            processor.onNext(5);
            processor.onComplete();
        };
        Runnable r2 = () -> processor.subscribe(subscriber);

        List<Runnable> runnables = Arrays.asList(r1, r2);
        Collections.shuffle(runnables);
        runnables.forEach(r -> {
            new Thread(r).start();
        });

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertCompletedSuccessfully();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1, 2, 3, 4, 5);
        }
    }

    @Test
    public void testSubscribingWhileEmittingItem() {
        SerializedProcessor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        processor.emitting = true;

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isFalse();

        processor.onSubscribe(mock(Subscription.class));

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isFalse();
    }

    @Test
    public void testSubscribingWhileEmittingItemAndCompletion() {
        SerializedProcessor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        processor.emitting = true;

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onComplete();

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isTrue();

        processor.onSubscribe(mock(Subscription.class));

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isTrue();
    }

    @Test
    public void testSubscribingWhileEmittingFailure() {
        SerializedProcessor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();

        processor.emitting = true;

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onError(new IOException("boom"));

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isTrue();

        processor.onSubscribe(mock(Subscription.class));

        assertThat(processor.emitting).isTrue();
        assertThat(processor.done).isTrue();
    }
}
