package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

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

    @RepeatedTest(10)
    public void testWithSequenceOfItems() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber
                .awaitNextItems(2, 2)
                .assertItems(1, 2)
                .awaitNextItems(8, 20)
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testWithRequest0() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.request(0);
        subscriber.awaitFailure(t -> assertThat(t)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request"));
    }

    @Test
    public void testWithShutdownExecutor() {
        ExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.shutdownNow();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(RejectedExecutionException.class, "");
    }

    @Test
    public void testWithRogueUpstreamSendingTooManyItems() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Flow.Subscription.class));
                for (int i = 0; i < 10000; i++) {
                    subscriber.onItem(i);
                }
                subscriber.onComplete();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .emitOn(executor)
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        await().untilAsserted(() -> subscriber.assertFailedWith(BackPressureFailure.class, ""));

        subscriber.assertFailedWith(BackPressureFailure.class, "");
    }

}
