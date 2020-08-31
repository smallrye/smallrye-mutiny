package io.smallrye.mutiny.operators;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
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

}
