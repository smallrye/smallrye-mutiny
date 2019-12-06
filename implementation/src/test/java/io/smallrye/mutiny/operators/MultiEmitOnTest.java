package io.smallrye.mutiny.operators;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiEmitOnTest {

    private ExecutorService executor;

    @BeforeMethod
    public void init() {
        executor = Executors.newFixedThreadPool(4);
    }

    @AfterMethod
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testWithSequenceOfItems() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(2);
        await().until(() -> subscriber.items().size() == 2);
        subscriber.assertReceived(1, 2);
        subscriber.request(20);
        await().until(() -> subscriber.items().size() == 10);
        subscriber.assertReceived(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void testWithRequest0() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .emitOn(executor)
                .subscribe().withSubscriber(MultiAssertSubscriber.create());

        subscriber.request(0);
        subscriber.await()
                .assertHasFailedWith(IllegalArgumentException.class, "request");
    }

}
