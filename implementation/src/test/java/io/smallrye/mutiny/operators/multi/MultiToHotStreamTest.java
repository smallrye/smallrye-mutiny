package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiToHotStreamTest {

    @Test
    public void testWithTwoSubscribers() {
        UnicastProcessor<String> processor = UnicastProcessor.create();

        Multi<String> multi = processor.map(s -> s).toHotStream();
        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
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
    public void testWithTwoSubscribersDeprecated() {
        UnicastProcessor<String> processor = UnicastProcessor.create();

        Multi<String> multi = processor.map(s -> s).toHotStream();
        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
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
    public void testSubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        Multi<String> multi = processor.map(s -> s).toHotStream();
        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onComplete();

        AssertSubscriber<String> subscriber3 = multi.subscribe()
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
        Multi<String> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onError(new Exception("boom"));

        AssertSubscriber<String> subscriber3 = multi.subscribe()
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
        Multi<Integer> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();
        processor.onError(new Exception("boom"));

        subscriber.assertCompleted().assertItems(1, 2);
    }

    @Test
    public void testNoItemAfterCancellation() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();
        Multi<String> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertItems("one");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
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
        Multi<String> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
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
        Multi<String> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
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
        Multi<String> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
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
    public void testWhenSubscriberDoesNotHaveRequestedEnough() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<Integer> s1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> s2 = multi.subscribe()
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
    public void testWithTransformToMultiAndMerge() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = processor.map(s -> s).toHotStream();

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        multi
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
                .select().first(100)
                .toHotStream();
        Thread.sleep(50); // NOSONAR

        AssertSubscriber<Long> subscriber = ticks.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion(Duration.ofSeconds(10))
                .assertCompleted();

        List<Long> items = subscriber.getItems();
        assertThat(items).isNotEmpty().doesNotContain(0L, 1L, 2L, 3L, 4L);
    }
}
