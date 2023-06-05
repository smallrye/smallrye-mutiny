package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.MultiSelectWhereOp;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.test.Mocks;

public class MultiSelectWhereAndWhenTest {

    @Test
    public void testThatPredicateCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .select().where(null));
    }

    @Test
    public void testThatLimitCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .select().where(x -> x == 2, -1));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .select().when(null));
    }

    @Test
    public void testThatSubscriberCannotBeNull() {
        assertThrows(NullPointerException.class, () -> {
            Multi<Integer> multi = Multi.createFrom().range(1, 4);
            MultiSelectWhereOp<Integer> filter = new MultiSelectWhereOp<>(multi, x -> x % 2 == 0);
            filter.subscribe(null);
        });
    }

    @Test
    public void cannotRequestZeroItems() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 4)
                .filter(n -> n % 2 == 0)
                .subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(0);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .select().where(test).collect().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithPredicateAndLimit() {
        Predicate<Integer> test = x -> x % 2 != 0;
        MultiOnCancellationSpy<Integer> spy = Spy
                .onCancellation(Multi.createFrom().range(1, 10));
        assertThat(spy
                .select().where(test, 2).collect().asList()
                .await().indefinitely()).containsExactly(1, 3);

        assertThat(spy.isCancelled()).isTrue();
    }

    @Test
    public void testFilteringWithPredicateAndZeroAsLimit() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 10)
                .select().where(test, 0).collect().asList()
                .await().indefinitely()).isEmpty();
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .select().when(
                        x -> Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collect().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilterShortcut() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .filter(test)
                .collect().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithDownstreamRequestingMax() {
        Predicate<Integer> test = x -> x % 2 != 0;
        LongAdder numberOfRequests = new LongAdder();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100000)
                .onRequest().invoke(numberOfRequests::increment)
                .select().where(test)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertCompleted();
        assertThat(numberOfRequests.longValue()).isEqualTo(1L);
    }

    @Test
    public void testNoEmissionAfterCompletion() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiSelectWhereOp<Integer> filter = new MultiSelectWhereOp<>(processor, x -> x % 2 == 0);
        Flow.Subscriber<Integer> subscriber = Mocks.subscriber();
        filter.subscribe(subscriber);

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onComplete();
        processor.onNext(5);
        processor.onNext(6);

        verify(subscriber).onNext(2);
        verify(subscriber).onNext(4);
        verify(subscriber, never()).onNext(6);
        verify(subscriber).onComplete();
        verify(subscriber, never()).onError(any(Throwable.class));

    }

    @Test
    public void testNoEmissionAfterFailure() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiSelectWhereOp<Integer> filter = new MultiSelectWhereOp<>(processor, x -> x % 2 == 0);
        Flow.Subscriber<Integer> subscriber = Mocks.subscriber();
        filter.subscribe(subscriber);

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onError(new IOException("boom"));
        processor.onNext(5);
        processor.onNext(6);

        verify(subscriber).onNext(2);
        verify(subscriber).onNext(4);
        verify(subscriber, never()).onNext(6);
        verify(subscriber, never()).onComplete();
        verify(subscriber).onError(any(IOException.class));

    }

    @Test
    public void testNoEmissionAfterCancellation() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> subscriber = processor.select().where(x -> x % 2 == 0)
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        subscriber.cancel();
        processor.onNext(5);
        processor.onNext(6);
        processor.onComplete();
        subscriber.assertItems(2, 4)
                .assertNotTerminated();
    }

    @Test
    public void testWithPredicateThrowingException() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> subscriber = processor.select().where(x -> {
            if (x == 3) {
                throw new IllegalArgumentException("boom");
            }
            return x % 2 == 0;
        })
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        processor.onNext(5);
        processor.onNext(6);
        processor.onComplete();
        subscriber.assertItems(2)
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }
}
