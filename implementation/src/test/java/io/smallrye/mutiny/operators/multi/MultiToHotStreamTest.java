package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiToHotStreamTest {

    @Test
    public void testWithTwoSubscribers() {
        UnicastProcessor<String> processor = UnicastProcessor.create();

        Multi<String> multi = processor.map(s -> s).transform().toHotStream();
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
                .assertReceived("one", "two", "three", "four")
                .assertCompletedSuccessfully();

        subscriber2
                .assertReceived("four")
                .assertCompletedSuccessfully();
    }

    @Test
    public void testSubscriptionAfterCompletion() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();

        Multi<String> multi = processor.map(s -> s).transform().toHotStream();
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
        Multi<String> multi = processor.map(s -> s).transform().toHotStream();

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
        Multi<Integer> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();
        processor.onError(new Exception("boom"));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2);
    }

    @Test
    public void testNoItemAfterCancellation() {
        BroadcastProcessor<String> processor = BroadcastProcessor.create();
        Multi<String> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        processor.onNext("one");

        subscriber1.assertReceived("one");

        AssertSubscriber<String> subscriber2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

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
        Multi<String> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

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
        Multi<String> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

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
        Multi<String> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<String> subscriber1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));

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
    public void testWhenSubscriberDoesNotHaveRequestedEnough() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<Integer> s1 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        AssertSubscriber<Integer> s2 = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(4));

        for (int i = 0; i < 10; i++) {
            processor.onNext(i);
        }
        processor.onComplete();

        s1.assertCompletedSuccessfully()
                .assertReceived(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        s2.assertHasFailedWith(BackPressureFailure.class, "request");
    }

    @Test
    public void testWithTransformToMultiAndMerge() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        Multi<Integer> multi = processor.map(s -> s).transform().toHotStream();

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        multi
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
                .transform().byTakingFirstItems(100)
                .transform().toHotStream();
        Thread.sleep(50); // NOSONAR

        AssertSubscriber<Long> subscriber = ticks.subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .await(Duration.ofSeconds(10))
                .assertCompletedSuccessfully();

        List<Long> items = subscriber.items();
        assertThat(items).isNotEmpty().doesNotContain(0L, 1L, 2L, 3L, 4L);
    }
}
