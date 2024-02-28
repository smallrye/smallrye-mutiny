package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

public class MultiOnFailureRetryUntilTest {

    private final Predicate<Throwable> retryTwice = new Predicate<Throwable>() {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public boolean test(Throwable throwable) {
            int attempt = counter.getAndIncrement();
            return attempt < 2;
        }
    };

    private final Predicate<Throwable> retryOnIoException = throwable -> throwable instanceof IOException;

    @Test
    public void testWithoutFailure() {
        Multi<Integer> upstream = Multi.createFrom().range(0, 4);
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> true)
                .subscribe().withSubscriber(subscriber);
        subscriber.assertItems(0, 1, 2, 3).assertCompleted();
    }

    @Test
    public void testInfiniteRetry() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> true)
                .subscribe(subscriber);

        subscriber.assertItems(0, 1, 0, 1, 2, 3).assertCompleted();
    }

    @Test
    public void testTwoRetriesAndGiveUp() {
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            em.emit(0);
            em.emit(1);
            em.fail(new Exception("boom"));
        });
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        upstream
                .onFailure().retry().until(retryTwice)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(Exception.class, "boom").assertItems(0, 1, 0, 1, 0, 1);
    }

    @Test
    public void testRetryOnSpecificException() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (attempt == 1) {
                em.fail(new IOException("boom"));
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(retryOnIoException).subscribe().withSubscriber(subscriber);

        subscriber
                .assertItems(0, 1, 0, 1, 2, 3)
                .assertCompleted();
    }

    @Test
    public void testRetryOnSpecificExceptionAndNotOther() {
        final IOException exception = new IOException("boom");
        final IllegalStateException ise = new IllegalStateException("kaboom");

        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int attempt = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (attempt == 1) {
                em.fail(exception);
            }
            em.emit(2);
            em.emit(3);
            em.fail(ise);
        });
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(retryOnIoException)
                .subscribe().withSubscriber(subscriber);

        subscriber
                .assertFailedWith(IllegalStateException.class, "kaboom")
                .assertItems(0, 1, 0, 1, 2, 3);
    }

    @Test
    public void testUnsubscribeFromRetry() {
        UnicastProcessor<Integer> processor = UnicastProcessor.create();

        AssertSubscriber<Integer> subscriber = Multi.createFrom().publisher(processor)
                .onFailure().retry().until(retryTwice)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        processor.onNext(1);
        subscriber.cancel();
        processor.onNext(2);
        assertThat(subscriber.getItems()).hasSize(1);
        subscriber.assertNotTerminated();
    }

    @Test
    public void testWithPredicateThrowingException() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> {
                    throw new IllegalStateException("boom");
                })
                .subscribe().withSubscriber(subscriber);
        subscriber.assertItems(0, 1).assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testWithPredicateReturningFalse() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> upstream = Multi.createFrom().emitter(em -> {
            int i = count.incrementAndGet();
            em.emit(0);
            em.emit(1);
            if (i == 1) {
                em.fail(new Exception("boom"));
                return;
            }
            em.emit(2);
            em.emit(3);
            em.complete();
        });

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        upstream
                .onFailure().retry().until(t -> false)
                .subscribe().withSubscriber(subscriber);
        subscriber.assertItems(0, 1).assertFailedWith(Exception.class, "boom");
    }

    @Test
    public void testWithBackoffAndUntilAndAlwaysTruePredicate() {
        AtomicInteger counter = new AtomicInteger();
        ArrayList<Long> timestamps = new ArrayList<>();
        AssertSubscriber<Integer> sub = Multi.createFrom().<Integer> emitter(emitter -> {
            timestamps.add(System.currentTimeMillis());
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            if (counter.incrementAndGet() == 5) {
                emitter.complete();
            } else {
                emitter.fail(new IOException("boom"));
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofSeconds(1)).withJitter(0).until(err -> true)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(256);
        sub.awaitCompletion();
        List<Integer> items = sub.getItems();
        assertThat(items)
                .hasSize(15)
                .startsWith(1, 2, 3)
                .endsWith(1, 2, 3);

        assertThat(timestamps)
                .hasSize(5);
        assertThat(timestamps.get(4) - timestamps.get(3))
                .isGreaterThan(timestamps.get(3) - timestamps.get(2));
        assertThat(timestamps.get(3) - timestamps.get(2))
                .isGreaterThan(timestamps.get(2) - timestamps.get(1));
    }

    @Test
    public void testWithBackoffAndUntilAndEventualFailure() {
        AtomicInteger counter = new AtomicInteger();
        ArrayList<Long> timestamps = new ArrayList<>();
        AssertSubscriber<Integer> sub = Multi.createFrom().<Integer> emitter(emitter -> {
            timestamps.add(System.currentTimeMillis());
            emitter.emit(1);
            emitter.emit(2);
            emitter.emit(3);
            if (counter.incrementAndGet() == 5) {
                emitter.complete();
            } else {
                emitter.fail(new IOException("boom"));
            }
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofSeconds(1)).withJitter(0)
                .until(err -> counter.get() < 3)
                .subscribe().withSubscriber(AssertSubscriber.create());

        sub.request(256);
        sub.awaitFailure().assertFailedWith(IOException.class, "boom");

        assertThat(sub.getItems())
                .hasSize(9)
                .startsWith(1, 2, 3)
                .endsWith(1, 2, 3);

        assertThat(timestamps)
                .hasSize(3);
        assertThat(timestamps.get(2) - timestamps.get(1))
                .isGreaterThan(timestamps.get(1) - timestamps.get(0));
    }
}
