package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiSkipUntilOtherOp;
import io.smallrye.mutiny.subscription.MultiEmitter;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class MultiSkipTest {

    @AfterEach
    public void cleanup() {
        Infrastructure.clearInterceptors();
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    public void testSimpleSkip() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().first(1)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testSkipFirst() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().first()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testSkipFirstZero() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().first(0)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void testSimpleSkipLast() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().last(1)
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    public void testSkipLast() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().last()
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly(1, 2, 3);
    }

    @Test
    public void testSimpleSkipZeroLast() {
        List<Integer> list = Multi.createFrom().range(1, 5)
                .skip().last(0)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly(1, 2, 3, 4);
    }

    @Test
    public void testSkipOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .skip().first()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipLastOnUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .skip().last()
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom")
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipAll() {
        Multi.createFrom().range(1, 5)
                .skip().first(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipLastAll() {
        Multi.createFrom().range(1, 5)
                .skip().last(4)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testInvalidSkipNumber() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3

                ).skip().first(-1));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Multi.createFrom().items(1, 2, 3).skip().last(-1));
    }

    @Test
    public void testSkipLastWithBackPressure() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0);

        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Multi.createFrom().<Integer> emitter(emitter::set)
                .skip().last(3)
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
                .assertItems(1);

        emitter.get().emit(5).emit(6).emit(7).emit(8).emit(9).emit(10).complete();

        subscriber.request(5)
                .assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSkipSomeLastItems() {
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().range(1, 11)
                .skip().last(3)
                .subscribe(subscriber);

        subscriber.assertCompleted()
                .assertItems(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void testSkipWhileWithMethodThrowingException() {
        Multi.createFrom().range(1, 10).skip().first(i -> {
            throw new IllegalStateException("boom");
        }).subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IllegalStateException.class, "boom");
    }

    @Test
    public void testSkipWhileWithUpstreamFailure() {
        Multi.createFrom().<Integer> failure(new IOException("boom"))
                .skip().first(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testSkipWhileWithNullMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().skip().first((Predicate<? super Object>) null));
    }

    @Test
    public void testSkipDurationWithNullAsDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().skip().first((Duration) null));
    }

    @Test
    public void testSkipWhile() {
        Multi.createFrom().range(1, 10).skip().first(i -> i < 5)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(5, 6, 7, 8, 9);
    }

    @Test
    public void testSkipWhileNone() {
        Multi.createFrom().items(1, 2, 3, 4).skip().first(i -> false)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertItems(1, 2, 3, 4);
    }

    @Test
    public void testSkipWhileAll() {
        Multi.createFrom().items(1, 2, 3, 4).skip().first(i -> true)
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipWhileSomeWithBackPressure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().items(1, 2, 3, 4)
                .skip().first(i -> i < 3)
                .subscribe().withSubscriber(AssertSubscriber.create(0));

        subscriber.assertNotTerminated()
                .assertHasNotReceivedAnyItem();

        subscriber.request(1);

        subscriber.assertNotTerminated()
                .assertItems(3);

        subscriber.request(2);

        subscriber.assertCompleted()
                .assertItems(3, 4);
    }

    @Test
    public void testSkipByTime() {
        Multi.createFrom().range(1, 100)
                .skip().first(Duration.ofMillis(2000))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testSkipByTimeWithInvalidDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().item(1).skip().first(Duration.ofMillis(-1)));
    }

    @Test
    public void testMultiSkipUntilPublisherOpeningOnItem() {
        Multi<Integer> upstream = Multi.createFrom().items(1, 2, 3, 4, 5, 6);
        new MultiSkipUntilOtherOp<>(upstream, Multi.createFrom().item(0))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertItems(1, 2, 3, 4, 5, 6)
                .assertCompleted();
    }

    @Test
    public void testMultiSkipUntilPublisherOpeningOnCompletion() {
        Multi<Integer> upstream = Multi.createFrom().items(1, 2, 3, 4, 5, 6);
        new MultiSkipUntilOtherOp<>(upstream, Multi.createFrom().empty())
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertItems(1, 2, 3, 4, 5, 6)
                .assertCompleted();
    }

    @Test
    public void testMultiSkipUntilPublisherWithOtherFailing() {
        Multi<Integer> upstream = Multi.createFrom().items(1, 2, 3, 4, 5, 6);
        new MultiSkipUntilOtherOp<>(upstream, Multi.createFrom().failure(new IOException("boom")))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testMultiSkipUntilPublisherWithUpstreamFailing() {
        Multi<Integer> upstream1 = Multi.createFrom().items(1, 2, 3, 4, 5, 6);
        Multi<Integer> upstream2 = Multi.createFrom().failure(new TestException("boom"));
        new MultiSkipUntilOtherOp<>(Multi.createBy().concatenating().streams(upstream1, upstream2),
                Multi.createFrom().item(0))
                .subscribe().withSubscriber(AssertSubscriber.create(10))
                .assertItems(1, 2, 3, 4, 5, 6)
                .assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testMultiSkipUntilPublisherWithDownstreamCancellationAndVerifyOtherIsCancelled() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        AtomicBoolean otherCancelled = new AtomicBoolean();
        Multi<Long> upstream = Multi.createFrom().ticks().every(Duration.ofMillis(10))
                .onOverflow().drop()
                .onCancellation().invoke(() -> upstreamCancelled.set(true));

        AssertSubscriber<Long> subscriber = new MultiSkipUntilOtherOp<>(upstream,
                Multi.createFrom().nothing().onCancellation().invoke(() -> otherCancelled.set(true)))
                .subscribe().withSubscriber(AssertSubscriber.create(1));

        subscriber.cancel();
        assertThat(upstreamCancelled).isTrue();
        assertThat(otherCancelled).isTrue();

    }

    @Test
    public void testItDoesNotRequest0() {
        AtomicBoolean called = new AtomicBoolean();
        Infrastructure.setDroppedExceptionHandler(t -> called.set(true));
        Multi.createFrom().range(1, 10)
                .skip().first(3)
                .select().where(n -> n % 2 == 0)
                .onItem().transform(n -> n * 10)
                .subscribe().withSubscriber(AssertSubscriber.create(100))

                .awaitCompletion()
                .assertItems(40, 60, 80);

        assertThat(called).isFalse();
    }

}
