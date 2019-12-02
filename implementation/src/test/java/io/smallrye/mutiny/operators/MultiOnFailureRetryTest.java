package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.MultiRetry;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiOnFailureRetryTest {

    private AtomicInteger numberOfSubscriptions;
    private Multi<Integer> failing;

    @BeforeMethod
    public void init() {
        numberOfSubscriptions = new AtomicInteger();
        failing = Multi.createFrom()
                .<Integer> emitter(emitter -> emitter.emit(1).emit(2).emit(3).fail(new IOException("boom")))
                .on().subscription(s -> numberOfSubscriptions.incrementAndGet());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatUpstreamCannotBeNull() {
        new MultiRetry<>(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheNumberOfAttemptMustBePositive() {
        Multi.createFrom().nothing()
                .onFailure().retry().atMost(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatTheNumberOfAttemptMustBePositive2() {
        Multi.createFrom().nothing()
                .onFailure().retry().atMost(0);
    }

    @Test
    public void testNoRetryOnNoFailure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(5);

        Multi.createFrom().range(1, 4)
                .onFailure().retry().atMost(5)
                .subscribe().with(subscriber);

        subscriber
                .assertReceived(1, 2, 3)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testWithASingleRetry() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(10);

        failing
                .onFailure().retry().atMost(1)
                .subscribe().with(subscriber);

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
                .subscribe().with(subscriber);

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
                .subscribe().with(subscriber);

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
                .subscribe().with(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

}
