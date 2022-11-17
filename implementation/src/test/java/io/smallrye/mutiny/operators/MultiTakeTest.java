package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiTakeTest {

    @Test
    public void testSimpleTake() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().first(1)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1);
    }

    @Test
    public void testTakeZero() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().first(0)
                .collect().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testSimpleTakeLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().last(1)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(4);
    }

    @Test
    public void testSimpleTakeZeroLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).select().last(0)
                .collect().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testTakeOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).select().first(1)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeLastOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).select().last(1)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeAll() {
        Multi.createFrom().range(1, 5).select().first(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testTakeLastAll() {
        Multi.createFrom().range(1, 5).select().last(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testInvalidTakeNumber() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).select().first(-1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).select().last(-1));
    }

    @Test
    public void testTakeLastWithBackPressure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .select().last(3)
                .subscribe(subscriber);

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(1).emit(2);

        subscriber.request(2)
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(3).emit(4);

        subscriber.request(5)
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        emitter.get().emit(5).emit(6).emit(7).emit(8).emit(9).emit(10).complete();

        subscriber.request(5)
                .assertCompleted()
                .assertItems(8, 9, 10);
    }

    @Test
    public void testTakeSomeLastItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 11)
                .select().last(3)
                .subscribe(subscriber);

        subscriber.assertCompleted()
                .assertItems(8, 9, 10);
    }

    @Test
    public void testTakeWhileWithMethodThrowingException() {
        Multi.createFrom().range(1, 10).select().where(i -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testTakeWhileWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .select().where(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testTakeWhileWithNullMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().select().where(null));
    }

    @Test
    public void testTakeWhile() {
        Multi.createFrom().range(1, 10).select().where(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testTakeWhileNone() {
        Multi.createFrom().items(1, 2, 3, 4).select().where(i -> false)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeWhileAll() {
        Multi.createFrom().items(1, 2, 3, 4).select().where(i -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testTakeWhileSomeWithBackPressure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4).select()
                .where(i -> i < 3)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        subscriber.request(1);

        subscriber.assertNotTerminated()
                .assertItems(1);

        subscriber.request(2);

        subscriber.assertCompleted()
                .assertItems(1, 2);
    }

    @Test
    public void testLimitingInfiniteStream() {
        Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .select().first(5)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitCompletion()
                .assertItems(0L, 1L, 2L, 3L, 4L);
    }

    @Test
    public void testTakeByTime() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100).select()
                .first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(10);
    }

    @Test
    public void testTakeByTimeWithFailure() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().failure(new TestException("boom")),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitFailure()
                .assertFailedWith(TestException.class, "boom");

        assertThat(subscriber.getItems()).hasSize(4);
    }

    @Test
    public void testTakeByTimeWithCancellation() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(4));

        await().until(() -> subscriber.getItems().size() == 4);
        subscriber.cancel();

        assertThat(subscriber.getItems()).hasSize(4);
    }

    @Test
    public void testTakeByTimeWithImmediateCancellation() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 5),
                Multi.createFrom().range(5, 10));
        AssertSubscriber<Integer> subscriber = multi
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(new AssertSubscriber<>(4, true));

        subscriber.assertSubscribed()
                .assertNotTerminated()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeByTimeWithRogueUpstreamSendingFailureAfterCompletion() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onComplete();
                subscriber.onItem(3);
                subscriber.onError(new IOException("boom"));
            }
        };
        AssertSubscriber<Integer> subscriber = rogue
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitCompletion();

        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
    public void testTakeByTimeWithRogueUpstreamSendingCompletionAfterFailure() {
        Multi<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onError(new IOException("boom"));
                subscriber.onItem(3);
                subscriber.onComplete();
            }
        };
        AssertSubscriber<Integer> subscriber = rogue
                .select().first(Duration.ofMillis(1000))
                .subscribe().withSubscriber(AssertSubscriber.create(100))
                .awaitFailure()
                .assertFailedWith(IOException.class, "boom");

        assertThat(subscriber.getItems()).hasSize(2);
    }

    @Test
    public void testSkipByTimeWithInvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).select().first(Duration.ofMillis(-1)));
    }

}
