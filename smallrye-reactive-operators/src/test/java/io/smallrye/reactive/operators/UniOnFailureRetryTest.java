package io.smallrye.reactive.operators;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

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
        Uni.createFrom().result(1)
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);
        ts.assertResult(1);

    }

    @Test
    public void testWithOneRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().result(() -> {
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
                .assertResult(1);

    }

    @Test
    public void testWithInfiniteRetry() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().result(() -> {
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
                .assertResult(10);
    }

    @Test
    public void testWithMapperFailure() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().result(1)
                .onResult().consume(input -> {
            if (count.incrementAndGet() < 2) {
                throw new RuntimeException("boom");
            }
        })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertResult(1);
    }
}
