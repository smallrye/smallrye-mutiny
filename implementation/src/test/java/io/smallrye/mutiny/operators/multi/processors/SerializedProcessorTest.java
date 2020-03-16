package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.reactivestreams.Processor;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
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

    @Test(invocationCount = 20)
    public void verifyOnNextThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> processor.onNext(1);
        Runnable r2 = () -> processor.onNext(2);

        new Thread(r1).start();
        new Thread(r2).start();

        await().until(() -> subscriber.items().size() == 2);

        subscriber
                .assertSubscribed()
                .assertNotTerminated();

        List<Integer> items = subscriber.items();
        assertThat(items).hasSize(2).contains(1, 2);
    }

    @Test(invocationCount = 20)
    public void verifyOnErrorThreadSafety() {
        Exception failure = new Exception("boom");
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> processor.onError(failure);
        Runnable r2 = () -> processor.onError(failure);

        new Thread(r1).start();
        new Thread(r2).start();

        subscriber
                .await()
                .assertSubscribed()
                .assertHasFailedWith(Exception.class, "boom");
    }

    @Test(invocationCount = 20)
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

        new Thread(r1).start();
        new Thread(r2).start();

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

    @Test(invocationCount = 20)
    public void verifyOnNextOnCompleteThreadSafety() {
        final Processor<Integer, Integer> processor = UnicastProcessor.<Integer> create().serialized();
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(100);
        processor.subscribe(subscriber);

        Runnable r1 = () -> {
            processor.onNext(1);
            processor.onComplete();
        };
        Runnable r2 = processor::onComplete;

        new Thread(r1).start();
        new Thread(r2).start();

        subscriber.await();
        subscriber
                .assertSubscribed()
                .assertCompletedSuccessfully();

        if (subscriber.items().size() != 0) {
            assertThat(subscriber.items()).containsExactly(1);
        }
    }
}
