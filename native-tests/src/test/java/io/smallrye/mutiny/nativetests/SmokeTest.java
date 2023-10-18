package io.smallrye.mutiny.nativetests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class SmokeTest {

    @Test
    public void concatMap() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().range(1, 10_000)
                .onItem().transformToMultiAndConcatenate(n -> Multi.createFrom().range(n + 2, n + 4))
                .subscribe().withSubscriber(subscriber);

        subscriber.request(5);
        subscriber.assertItems(3, 4, 4, 5, 5);

        subscriber.request(Long.MAX_VALUE);
        subscriber.assertCompleted();
        assertEquals(19998, subscriber.getItems().size());
    }

    @Test
    public void emitter() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().<Integer> emitter(emitter -> {
            new Thread(() -> {
                Random random = new Random();
                for (int i = 0; i < 10_000; i++) {
                    emitter.emit(random.nextInt());
                }
                emitter.complete();
            }).start();
        }).subscribe().withSubscriber(subscriber);

        subscriber.request(Long.MAX_VALUE);
        subscriber.awaitCompletion();
        assertEquals(10_000, subscriber.getItems().size());
    }
}
