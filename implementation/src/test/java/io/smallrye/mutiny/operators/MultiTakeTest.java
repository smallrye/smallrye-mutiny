package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiTakeTest {

    @Test
    public void testSimpleTake() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().byTakingFirstItems(1)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(1);
    }

    @Test
    public void testTakeZero() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().byTakingFirstItems(0)
                .collectItems().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testSimpleTakeLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().byTakingLastItems(1)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(4);
    }

    @Test
    public void testSimpleTakeZeroLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().byTakingLastItems(0)
                .collectItems().asList().await().indefinitely();

        assertThat(list).isEmpty();
    }

    @Test
    public void testTakeOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).transform().byTakingFirstItems(1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeLastOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).transform().byTakingLastItems(1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeAll() {
        Multi.createFrom().range(1, 5).transform().byTakingFirstItems(4)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testTakeLastAll() {
        Multi.createFrom().range(1, 5).transform().byTakingLastItems(4)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testInvalidTakeNumber() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).transform().byTakingFirstItems(-1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).transform().byTakingLastItems(-1));
    }

    @Test
    public void testTakeLastWithBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .transform().byTakingLastItems(3)
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
                .assertCompletedSuccessfully()
                .assertReceived(8, 9, 10);
    }

    @Test
    public void testTakeSomeLastItems() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 11)
                .transform().byTakingLastItems(3)
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully()
                .assertReceived(8, 9, 10);
    }

    @Test
    public void testTakeWhileWithMethodThrowingException() {
        Multi.createFrom().range(1, 10).transform().byTakingItemsWhile(i -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testTakeWhileWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().byTakingItemsWhile(i -> i < 5)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWhileWithNullMethod() {
        Multi.createFrom().nothing().transform().byTakingItemsWhile(null);
    }

    @Test
    public void testTakeWhile() {
        Multi.createFrom().range(1, 10).transform().byTakingItemsWhile(i -> i < 5)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testTakeWhileNone() {
        Multi.createFrom().items(1, 2, 3, 4).transform().byTakingItemsWhile(i -> false)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testTakeWhileAll() {
        Multi.createFrom().items(1, 2, 3, 4).transform().byTakingItemsWhile(i -> true)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testTakeWhileSomeWithBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4).transform()
                .byTakingItemsWhile(i -> i < 3)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0));

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        subscriber.request(1);

        subscriber.assertNotTerminated()
                .assertReceived(1);

        subscriber.request(2);

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2);
    }

    @Test
    public void testLimitingInfiniteStream() {
        Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .transform().byTakingFirstItems(5)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(Long.MAX_VALUE))
                .await()
                .assertCompletedSuccessfully()
                .assertReceived(0L, 1L, 2L, 3L, 4L);
    }

    @Test
    public void testTakeByTime() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100).transform()
                .byTakingItemsFor(Duration.ofMillis(1000))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .await()
                .assertCompletedSuccessfully();

        assertThat(subscriber.items()).hasSize(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSkipByTimeWithInvalidDuration() {
        Multi.createFrom().item(1).transform().byTakingItemsFor(Duration.ofMillis(-1));
    }

}
