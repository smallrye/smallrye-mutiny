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
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
        Uni.createFrom().result(1)
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(ts);
        ts.assertResult(1);

    }

    @Test
    public void testWithOneRetry() {
        AssertSubscriber<Integer> ts = AssertSubscriber.create();

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
        AssertSubscriber<Integer> ts = AssertSubscriber.create();
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
                .onResult().peek(input -> {
            if (count.incrementAndGet() < 2) {
                throw new RuntimeException("boom");
            }
        })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create())
                .assertResult(1);
    }
}
