package io.smallrye.mutiny.operators;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.test.AssertSubscriber;

public class MultiEmitOnTest {

    private ExecutorService executor;

    @BeforeEach
    public void init() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testWithSequenceOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(2);
        await().until(() -> subscriber.items().size() == 2);
        subscriber.assertReceived(1, 2);
        subscriber.request(20);
        await().until(() -> subscriber.items().size() == 10);
        subscriber.assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testWithRequest0() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(0);
        subscriber.await()
                .assertHasFailedWith(IllegalArgumentException.class, "request");
    }

    @Test
    public void testWithShutdownExecutor() {
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.shutdownNow();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertHasFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testWithRogueUpstreamSendingTooManyItems() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                for (int i = 0; i < 10000; i++) {
                    subscriber.onItem(i);
                }
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        await().untilAsserted(() -> subscriber.assertHasFailedWith(BackPressureFailure.class, ""));

        subscriber.assertHasFailedWith(BackPressureFailure.class, "");
    }

}
