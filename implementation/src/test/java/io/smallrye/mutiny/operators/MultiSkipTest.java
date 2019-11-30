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

public class MultiSkipTest {

    @Test
    public void testSimpleSkip() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().bySkippingFirstItems(1)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testSkipZero() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().bySkippingFirstItems(0)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void testSimpleSkipLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().bySkippingLastItems(1)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    public void testSimpleSkipZeroLast() {
        List<Integer> list = Multi.createFrom().range(1, 5).transform().bySkippingLastItems(0)
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void testSkipOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).transform().bySkippingFirstItems(1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipLastOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom")).transform().bySkippingLastItems(1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipAll() {
        Multi.createFrom().range(1, 5).transform().bySkippingFirstItems(4)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipLastAll() {
        Multi.createFrom().range(1, 5).transform().bySkippingLastItems(4)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testInvalidSkipNumber() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).transform().bySkippingFirstItems(-1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).transform().bySkippingLastItems(-1));
    }

    @Test
    public void testSkipLastWithBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(0);

        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .transform().bySkippingLastItems(3)
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
                .assertReceived(1);

        emitter.get().emit(5).emit(6).emit(7).emit(8).emit(9).emit(10).complete();

        subscriber.request(5)
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSkipSomeLastItems() {
        MultiAssertSubscriber<Integer> subscriber = MultiAssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 11)
                .transform().bySkippingLastItems(3)
                .subscribe(subscriber);

        subscriber.assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSkipWhileWithMethodThrowingException() {
        Multi.createFrom().range(1, 10).transform().bySkippingItemsWhile(i -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testSkipWhileWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .transform().bySkippingItemsWhile(i -> i < 5)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertHasFailedWith(IOException.class, "boom");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSkipWhileWithNullMethod() {
        Multi.createFrom().nothing().transform().bySkippingItemsWhile(null);
    }

    @Test
    public void testSkipWhile() {
        Multi.createFrom().range(1, 10).transform().bySkippingItemsWhile(i -> i < 5)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(5, 6, 7, 8, 9);
    }

    @Test
    public void testSkipWhileNone() {
        Multi.createFrom().items(1, 2, 3, 4).transform().bySkippingItemsWhile(i -> false)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertReceived(1, 2, 3, 4);
    }

    @Test
    public void testSkipWhileAll() {
        Multi.createFrom().items(1, 2, 3, 4).transform().bySkippingItemsWhile(i -> true)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipWhileSomeWithBackPressure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4).transform()
                .bySkippingItemsWhile(i -> i < 3)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(0));

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        subscriber.request(1);

        subscriber.assertNotTerminated()
                .assertReceived(3);

        subscriber.request(2);

        subscriber.assertCompletedSuccessfully()
                .assertReceived(3, 4);
    }

    @Test
    public void testSkipByTime() {
        Multi.createFrom().range(1, 100)
                .transform().bySkippingItemsFor(Duration.ofMillis(2000))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10))
                .assertCompletedSuccessfully()
                .assertHasNotReceivedAnyItem();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSkipByTimeWithInvalidDuration() {
        Multi.createFrom().item(1).transform().bySkippingItemsFor(Duration.ofMillis(-1));
    }

}
