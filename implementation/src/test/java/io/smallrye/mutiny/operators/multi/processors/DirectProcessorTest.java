package io.smallrye.mutiny.operators.multi.processors;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class DirectProcessorTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void testThatNullCannotBeEmitted() {
        DirectProcessor.create().onNext(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testThatFailureCannotBeNull() {
        DirectProcessor.create().onError(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testThatSubscriptionCannotBeNull() {
        DirectProcessor.create().onSubscribe(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testThatSubscriberCannotBeNull() {
        DirectProcessor.create().subscribe(null);
    }

    @Test
    public void testRegularInteractionsWithUnboundedRequests() {
        DirectProcessor<Integer> processor = DirectProcessor.create();
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);
        processor.subscribe(subscriber);

        processor.onNext(1);
        processor.onNext(2);

        subscriber.assertReceived(1, 2);

        processor.onNext(3);
        processor.onComplete();

        subscriber.assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testRegularInteractionWithBackPressure() {
        DirectProcessor<Integer> processor = DirectProcessor.create();

        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(0);
        processor.subscribe(subscriber);

        subscriber.request(10);

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();

        subscriber.assertReceived(1, 2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWhenNotEnoughRequests() {
        DirectProcessor<Integer> processor = DirectProcessor.create();

        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(1);
        processor.subscribe(subscriber);

        processor.onNext(1);
        processor.onNext(2);
        processor.onComplete();

        subscriber.assertHasFailedWith(IllegalStateException.class, "requests");
    }

    @Test
    public void testFailurePropagation() {
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(100);
        DirectProcessor<Integer> processor = DirectProcessor.create();
        processor.subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();

        processor.onNext(1);
        processor.onNext(2);

        subscriber.assertReceived(1, 2)
                .assertNotTerminated();

        processor.onNext(3);
        processor.onError(new RuntimeException("boom"));

        Throwable failure = processor.getFailure();

        subscriber.assertHasFailedWith(RuntimeException.class, "boom");
        assertThat(failure).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");
    }

    @Test
    public void testWhenTheStreamIsAlreadyTerminatedWithAFailure() {
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(100);

        DirectProcessor<Integer> processor = DirectProcessor.create();
        processor.onError(new RuntimeException("boom"));

        processor.subscribe(subscriber);

        Throwable failure = processor.getFailure();
        assertThat(failure).isInstanceOf(RuntimeException.class).hasMessageContaining("boom");
        subscriber.assertHasFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testWhenTheStreamIsAlreadyTerminatedWithCompletion() {
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(100);

        DirectProcessor<Integer> processor = DirectProcessor.create();
        processor.onComplete();

        processor.subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithASubscriberCancellingImmediately() {
        MultiAssertSubscriber<Object> subscriber = new MultiAssertSubscriber<>(10, true);
        DirectProcessor<Integer> processor = DirectProcessor.create();
        processor.subscribe(subscriber);
        processor.onNext(1);
        subscriber
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated();
    }

    @Test
    public void testWithASubscriberCancellingAfterEmission() {
        MultiAssertSubscriber<Object> subscriber = MultiAssertSubscriber.create(100);
        DirectProcessor<Integer> processor = DirectProcessor.create();
        processor.subscribe(subscriber);
        processor.onNext(1);

        subscriber
                .assertReceived(1)
                .assertNotTerminated();
        subscriber.cancel();

        processor.onNext(2);

        subscriber
                .assertReceived(1)
                .assertNotTerminated();
    }

}
