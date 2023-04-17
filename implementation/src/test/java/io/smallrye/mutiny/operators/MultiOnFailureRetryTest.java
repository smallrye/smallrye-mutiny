package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.MultiRetry;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.unchecked.Unchecked;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiOnFailureRetryTest {

    private AtomicInteger numberOfSubscriptions;
    private Multi<Integer> failing;

    @BeforeEach
    public void init() {
        numberOfSubscriptions = new AtomicInteger();
        failing = Multi.createFrom()
                .<Integer> emitter(emitter -> emitter.emit(1).emit(2).emit(3).fail(new IOException("boom")))
                .onSubscription().invoke(s -> numberOfSubscriptions.incrementAndGet());
    }

    @Test
    public void testThatUpstreamCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new MultiRetry<>(null, null));
    }

    @Test
    public void testThatTheNumberOfAttemptMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().nothing()
                .onFailure().retry().atMost(-1));
    }

    @Test
    public void testThatTheNumberOfAttemptMustBePositive2() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().nothing()
                .onFailure().retry().atMost(0));
    }

    @Test
    public void testNoRetryOnNoFailure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(5);

        Multi.createFrom().range(1, 4)
                .onFailure().retry().atMost(5)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertItems(1, 2, 3)
                .assertCompleted();
    }

    @Test
    public void testWithASingleRetry() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        failing
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertSubscribed()
                .assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2, 3, 1, 2, 3);

        assertThat(numberOfSubscriptions).hasValue(2);
    }

    @Test
    public void testWithASingleRetryAndRequests() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        failing
                .onFailure().retry().atMost(1)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertSubscribed()
                .assertHasNotReceivedAnyItem()
                .request(4)
                .assertItems(1, 2, 3, 1)
                .request(2)
                .assertFailedWith(IOException.class, "boom")
                .assertItems(1, 2, 3, 1, 2, 3);

        assertThat(numberOfSubscriptions).hasValue(2);
    }

    @Test
    public void testRetryIndefinitely() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(20);

        failing.onFailure().retry().indefinitely()
                .subscribe().withSubscriber(subscriber);

        await().until(() -> subscriber.getItems().size() > 10);
        subscriber.cancel();

        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithRetryingGoingBackToSuccess() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure().retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testWithRetryWithBackoffWithPredicate() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    int counter = count.getAndIncrement();
                    if (counter == 3 || counter == 4) {
                        throw new IllegalArgumentException("must-retry");
                    }
                    if (counter == 5) {
                        throw new RuntimeException("boom");
                    }
                })
                .onFailure(IllegalArgumentException.class)
                .retry()
                .withBackOff(Duration.ofMillis(10)).atMost(3)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(RuntimeException.class, "boom")
                .assertItems(1, 2, 3);
    }

    @Test
    public void testThatYouCannotUseWhenIfBackoffIsConfigured() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item("hello")
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).when(t -> Multi.createFrom().item(t)));
    }

    @Test
    public void testThatYouCannotUseUntilIfBackoffIsConfigured() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item("hello")
                .onFailure().retry().withBackOff(Duration.ofSeconds(1)).until(t -> true));
    }

    @Test
    public void testJitterValidation() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item("hello")
                .onFailure().retry().withJitter(2));
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryAtMost() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyRuntimeException("boom");
                    }
                })
                .onFailure(MyRuntimeException.class).retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryAtMostAndBackOff() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyRuntimeException("boom");
                    }
                })
                .onFailure(MyRuntimeException.class).retry().withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong")).retry()
                .withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().withBackOff(Duration.ofMillis(2)).atMost(2)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitFailure()
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryWhen() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyRuntimeException("boom");
                    }
                })
                .onFailure(MyRuntimeException.class).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong"))
                .retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().when(t -> Multi.createFrom().items(1, 1, 1, 1))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void checkThatItDoesOnlyRetryOnMatchingExceptionWithRetryUntil() {
        AtomicInteger count = new AtomicInteger();

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyRuntimeException("boom");
                    }
                })
                .onFailure(MyRuntimeException.class).retry().until(t -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().contains("boom")).retry().until(t -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(ArithmeticException.class).retry().until(t -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> t.getMessage().equalsIgnoreCase("wrong"))
                .retry().until(t -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(RuntimeException.class, "boom");

        count.set(0);

        Multi.createFrom().items(1, 2, 3, 4)
                .onItem().invoke(Unchecked.consumer(i -> {
                    if (count.getAndIncrement() < 2) {
                        throw new MyException("boom");
                    }
                }))
                .onFailure(t -> {
                    throw new RuntimeException("expected");
                }).retry().until(t -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(CompositeException.class, "expected");
    }

    @Test
    public void rejectNullExecutors() {
        assertThatThrownBy(() -> Multi.createFrom().items(1, 2, 3)
                .onFailure().retry().withExecutor(null).withBackOff(Duration.ofMillis(100)).atMost(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("`executor` must not be `null`");
    }

    public static class MyException extends Exception {

        public MyException(String m) {
            super(m);
        }
    }

    public static class MyRuntimeException extends RuntimeException {

        public MyRuntimeException(String m) {
            super(m);
        }
    }

}
