package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.groups.MultiRetry;
import io.smallrye.reactive.test.MultiAssertSubscriber;

public class MultiOnFailureRetryTest {

    private AtomicInteger numberOfSubscriptions = new AtomicInteger();
    private Multi<Integer> failing = Multi.createFrom()
            .<Integer> emitter(emitter -> emitter.emit(1).emit(2).emit(3).fail(new IOException("boom")))
            .on().subscription(s -> numberOfSubscriptions.incrementAndGet());

    @Test(expected = IllegalArgumentException.class)
    public void testThatUpstreamCannotBeNull() {
        new MultiRetry<>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheNumberOfAttemptMustBePositive() {
        Multi.createFrom().nothing()
                .onFailure().retry().atMost(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatTheNumberOfAttemptMustBePositive2() {
        Multi.createFrom().nothing()
                .onFailure().retry().atMost(0);
    }

    @Test
    public void testNoRetryOnNoFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(5);

        Multi.createFrom().range(1, 4)
                .onFailure().retry().atMost(5)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithASingleRetry() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        failing
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2, 3, 1, 2, 3);

        assertThat(numberOfSubscriptions).hasValue(2);
    }

    @Test
    public void testWithASingleRetryAndRequests() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        failing
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .request(4)
                .assertReceived(1, 2, 3, 1)
                .assertHasNotFailed()
                .request(2)
                .assertHasFailedWith(IOException.class, "boom")
                .assertReceived(1, 2, 3, 1, 2, 3);

        assertThat(numberOfSubscriptions).hasValue(2);
    }

    @Test
    public void testRetryIndefinitely() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(20);

        failing.onFailure().retry().indefinitely()
                .subscribe().withSubscriber(subscriber);

        await().until(() -> subscriber.items().size() > 10);
        subscriber.cancel();

        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithRetryingGoingBackToSuccess() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().consume(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

}
