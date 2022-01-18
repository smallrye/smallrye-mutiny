package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.MultiRetryWhenOp;
import io.smallrye.mutiny.tuples.Tuple2;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiOnFailureRetryWhenTest {

    private AtomicInteger numberOfSubscriptions;
    private Multi<Integer> failingAfter2;
    private Multi<Integer> failingAfter1;

    @BeforeEach
    public void init() {
        numberOfSubscriptions = new AtomicInteger();
        failingAfter2 = Multi.createFrom()
                .<Integer> emitter(emitter -> emitter.emit(1).emit(2).fail(new IOException("boom")))
                .onSubscription().invoke(s -> numberOfSubscriptions.incrementAndGet());

        failingAfter1 = Multi.createBy().concatenating()
                .streams(Multi.createFrom().item(1), Multi.createFrom().failure(new RuntimeException("boom")));
    }

    @Test
    public void testThatUpstreamCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new MultiRetryWhenOp<>(null, null, v -> v));
    }

    @Test
    public void testThatStreamFactoryCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().nothing().onFailure().retry().when(null));
    }

    @Test
    public void testThatCancellingTheStreamCancelTheProducedWhenStream() {
        AtomicInteger cancelled = new AtomicInteger(0);
        Multi<Integer> when = Multi.createFrom().range(1, 10)
                .onCancellation().invoke(cancelled::incrementAndGet);

        AssertSubscriber<Integer> subscriber = failingAfter1
                .onFailure().retry().when(x -> when)
                .subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertItems(1, 1, 1)
                .cancel();

        assertThat(cancelled).hasValue(1);

        subscriber.cancel();
        assertThat(cancelled).hasValue(1);
    }

    @Test
    public void testWithOtherStreamFailing() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<Integer> multi = failingAfter1
                .onSubscription().invoke(sub -> subscribed.set(true))
                .onCancellation().invoke(() -> cancelled.set(true));

        AssertSubscriber<Integer> subscriber = multi
                .onFailure().retry().when(other -> Multi.createFrom().failure(new IllegalStateException("boom")))
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber
                .assertSubscribed()
                .assertFailedWith(IllegalStateException.class, "boom");

        assertThat(subscribed.get()).isFalse();
        assertThat(cancelled.get()).isFalse();
    }

    @Test
    public void testWhatTheWhenStreamFailsTheUpstreamIsCancelled() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<Integer> multi = failingAfter1
                .onSubscription().invoke(sub -> subscribed.set(true))
                .onCancellation().invoke(() -> cancelled.set(true));

        AtomicInteger count = new AtomicInteger();
        Multi<Integer> retry = multi
                .onFailure().retry()
                .when(other -> other.flatMap(l -> count.getAndIncrement() == 0 ? Multi.createFrom().item(l)
                        : Multi.createFrom().failure(new IllegalStateException("boom"))));

        AssertSubscriber<Integer> subscriber = retry.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertSubscribed()
                .assertItems(1, 1)
                .assertFailedWith(IllegalStateException.class, "boom");

        assertThat(subscribed.get()).isTrue();
        assertThat(cancelled.get()).isTrue();
    }

    @Test
    public void testCompletionWhenOtherStreamCompletes() {
        AtomicBoolean subscribed = new AtomicBoolean();
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<Integer> source = failingAfter1
                .onSubscription().invoke(sub -> subscribed.set(true))
                .onCancellation().invoke(() -> cancelled.set(true));

        Multi<Integer> retry = source
                .onFailure().retry().when(other -> Multi.createFrom().empty());

        AssertSubscriber<Integer> subscriber = retry.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertSubscribed()
                .assertCompleted();

        assertThat(subscribed.get()).isFalse();
        assertThat(cancelled.get()).isFalse();
    }

    @Test
    public void testAfterOnRetryAndCompletion() {
        AtomicBoolean sourceSubscribed = new AtomicBoolean();
        Multi<Integer> source = failingAfter1
                .onSubscription().invoke(sub -> sourceSubscribed.set(true));

        Multi<Integer> retry = source
                .onFailure().retry().when(other -> other.select().first());

        AssertSubscriber<Integer> subscriber = retry.subscribe()
                .withSubscriber(AssertSubscriber.create(10));
        subscriber
                .assertSubscribed()
                .assertItems(1, 1)
                .assertCompleted();
        assertThat(sourceSubscribed.get()).isTrue();
    }

    @Test
    public void testRepeat() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(11);

        failingAfter1
                .onFailure().retry().when(v -> Multi.createFrom().range(1, 11))
                .subscribe(subscriber);

        subscriber
                .assertItems(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
                .assertCompleted();
    }

    @Test
    public void testRepeatWithBackPressure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        failingAfter2
                .onFailure().retry().when(v -> Multi.createFrom().range(1, 6))
                .subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertNotTerminated()

                .request(1)
                .assertItems(1)
                .assertNotTerminated()

                .request(2)
                .assertItems(1, 2, 1)
                .assertNotTerminated()

                .request(5)
                .assertItems(1, 2, 1, 2, 1, 2, 1, 2)
                .assertNotTerminated()

                .request(10)
                .assertItems(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2)
                .assertCompleted();
    }

    @Test
    public void testWithOtherStreamBeingEmpty() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        failingAfter2
                .onFailure().retry().when(v -> Multi.createFrom().empty())
                .subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertCompleted();
    }

    @Test
    public void testWithOtherStreamFailing2() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        failingAfter2
                .onFailure().retry().when(v -> Multi.createFrom().failure(new RuntimeException("failed")))
                .subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(RuntimeException.class, "failed");
    }

    @Test
    public void testWithOtherStreamThrowingException() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        failingAfter2
                .onFailure().retry().when(v -> {
                    throw new RuntimeException("failed");
                })
                .subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(RuntimeException.class, "failed");

    }

    @Test
    public void testWithOtherStreamReturningNull() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        failingAfter2
                .onFailure().retry().when(v -> null)
                .subscribe(subscriber);

        subscriber
                .assertHasNotReceivedAnyItem()
                .assertFailedWith(NullPointerException.class, "");

    }

    @Test
    public void testWithOtherStreamFailingAfterAFewItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);

        failingAfter2.onFailure().retry().when(v -> v.map(a -> {
            throw new RuntimeException("failed");
        }))
                .subscribe(subscriber);

        subscriber.assertItems(1, 2)
                .assertFailedWith(RuntimeException.class, "failed");

    }

    @Test
    public void testInfiniteRetry() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        failingAfter2
                .onFailure().retry().when(v -> v)
                .subscribe(subscriber);

        subscriber.request(8);

        subscriber.assertItems(1, 2, 1, 2, 1, 2, 1, 2)
                .assertNotTerminated();

        subscriber.cancel();
    }

    Multi<String> getMultiWithManualExponentialRetry() {
        AtomicInteger i = new AtomicInteger();
        return Multi.createFrom().<String> emitter(s -> {
            if (i.incrementAndGet() == 4) {
                s.emit("hey");
            } else {
                s.fail(new RuntimeException("test " + i));
            }
        }).onFailure().retry().when(
                repeat -> Multi.createBy().combining().streams(repeat, Multi.createFrom().range(1, 4)).asTuple()
                        .map(Tuple2::getItem2)
                        .onItem().transformToUni(time -> Uni.createFrom().item(time)
                                .onItem().delayIt().by(Duration.ofMillis(time)))
                        .concatenate());
    }

    @Test
    public void testManualExponentialRetry() {
        AssertSubscriber<String> subscriber = getMultiWithManualExponentialRetry()
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber
                .awaitCompletion(Duration.ofSeconds(10))
                .assertItems("hey")
                .assertCompleted();
    }

    @Test
    public void testRetryWithRandomBackoff() {
        Exception exception = new IOException("boom retry");

        AssertSubscriber<Integer> subscriber = Multi.createFrom().<Integer> emitter(e -> {
            e.emit(0);
            e.emit(1);
            e.fail(exception);
        })
                .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofHours(1)).withJitter(0.1)
                .atMost(4)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        await().until(() -> subscriber.getItems().size() >= 10);
        subscriber
                .assertItems(0, 1, 0, 1, 0, 1, 0, 1, 0, 1) // Initial subscription + 4 retries
                .assertFailedWith(IOException.class, "boom retry")
                .awaitFailure(t -> {
                    // expecting an IllegalStateException with an info about the retries made
                    Throwable[] suppressed = exception.getSuppressed();
                    assertThat(suppressed.length).isEqualTo(1);
                    assertThat(suppressed).anyMatch(s -> s instanceof IllegalStateException &&
                            s.getMessage().equals("Retries exhausted: 4/4"));
                });

    }

    @Test
    public void testRetryWithRandomBackoffAndDefaultJitter() {
        Exception exception = new IOException("boom retry");
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating()
                .streams(Multi.createFrom().range(0, 2), Multi.createFrom().failure(exception))

                .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofHours(1))
                .atMost(4)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        await().until(() -> subscriber.getItems().size() >= 10);
        subscriber
                .assertItems(0, 1, 0, 1, 0, 1, 0, 1, 0, 1) // Initial subscription + 4 retries
                .assertFailedWith(IOException.class, "boom retry")
                .awaitFailure(t -> {
                    // expecting an IllegalStateException with an info about the retries made
                    Throwable[] suppressed = exception.getSuppressed();
                    assertThat(suppressed.length).isEqualTo(1);
                    assertThat(suppressed).anyMatch(s -> s instanceof IllegalStateException &&
                            s.getMessage().equals("Retries exhausted: 4/4"));
                });
    }

    @Test
    public void testRetryWithDefaultMax() {
        Exception exception = new IOException("boom retry");
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating()
                .streams(Multi.createFrom().range(0, 2), Multi.createFrom().failure(exception))
                .onFailure().retry().withBackOff(Duration.ofMillis(10)).withJitter(0.0)
                .atMost(4)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        await()
                .atMost(1, TimeUnit.MINUTES)
                .until(() -> subscriber.getItems().size() >= 10);
        subscriber
                .assertItems(0, 1, 0, 1, 0, 1, 0, 1, 0, 1) // Initial subscription + 4 retries
                .assertFailedWith(IOException.class, "boom retry")
                .awaitFailure(t -> {
                    // expecting an IllegalStateException with an info about the retries made
                    Throwable[] suppressed = exception.getSuppressed();
                    assertThat(suppressed.length).isEqualTo(1);
                    assertThat(suppressed).anyMatch(s -> s instanceof IllegalStateException &&
                            s.getMessage().equals("Retries exhausted: 4/4"));
                });
    }

}
