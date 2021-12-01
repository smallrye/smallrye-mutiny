package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class UniOnFailureRetryTest {

    @Test
    public void testInvalidNumberOfAttempts() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().nothing().onFailure().retry().atMost(-10));
    }

    @Test
    public void testInvalidNumberOfAttemptsWithZero() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().nothing().onFailure().retry().atMost(0));
    }

    @Test
    public void testNoRetryOnItem() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item(1)
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);
        subscriber.assertItem(1);

    }

    @Test
    public void testWithOneRetry() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();

        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(() -> {
            int i = count.getAndIncrement();
            if (i < 1) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompleted()
                .assertItem(1);

    }

    @Test
    public void testWithInfiniteRetry() {
        UniAssertSubscriber<Integer> subscriber = UniAssertSubscriber.create();
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(() -> {
            int i = count.getAndIncrement();
            if (i < 10) {
                throw new RuntimeException("boom");
            }
            return i;
        })
                .onFailure().retry().indefinitely()
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertCompleted()
                .assertItem(10);
    }

    @Test
    public void testWithMapperFailure() {
        AtomicInteger count = new AtomicInteger();
        Uni.createFrom().item(1)
                .onItem().invoke(input -> {
                    if (count.incrementAndGet() < 2) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertItem(1);
    }

    @Test
    public void testThatNumberOfRetryIfCorrect() {
        AtomicInteger calls = new AtomicInteger();

        UniAssertSubscriber<Integer> subscriber = Uni.createFrom().<Integer> item(() -> {
            calls.incrementAndGet();
            throw new RuntimeException("boom");
        })
                .onFailure().retry().atMost(3)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.awaitFailure();
        assertThat(calls).hasValue(4); // initial subscription + 3 retries
    }
}
