package io.smallrye.reactive.unimulti.operators;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Uni;

public class UniOnFailureRetryTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberOfAttempts() {
        Uni.createFrom().nothing().onFailure().retry().atMost(-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberOfAttemptsWithZero() {
        Uni.createFrom().nothing().onFailure().retry().atMost(0);
    }

    @Test
    public void testNoRetryOnResult() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().item(1)
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);
        ts.assertItem(1);

    }

    @Test
    public void testWithOneRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().deferredItem(() -> {
            int i = count.getAndIncrement();
            if (i < 1) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);

        ts
                .assertCompletedSuccessfully()
                .assertItem(1);

    }

    @Test
    public void testWithInfiniteRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().deferredItem(() -> {
            int i = count.getAndIncrement();
            if (i < 10) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().indefinitely()
                .subscribe().withSubscriber(ts);

        ts
                .assertCompletedSuccessfully()
                .assertItem(10);
    }

    @Test
    public void testWithMapperFailure() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(1)
                .onItem().consume(input -> {
                    if (count.incrementAndGet() < 2) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(1);
    }
}
