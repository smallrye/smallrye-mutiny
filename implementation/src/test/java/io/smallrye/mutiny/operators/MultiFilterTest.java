package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.MultiFilterOp;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.test.Mocks;

public class MultiFilterTest {

    @Test
    public void testThatPredicateCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(null));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .transform().byTestingItemsWith(null));
    }

    @Test
    public void testThatSubscriberCannotBeNull() {
        assertThrows(NullPointerException.class, () -> {
            Multi<Integer> multi = Multi.createFrom().range(1, 4);
            MultiFilterOp<Integer> filter = new MultiFilterOp<>(multi, x -> x % 2 == 0);
            filter.subscribe(null);
        });
    }

    @Test
    public void cannotRequestZeroItems() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 4)
                .filter(n -> n % 2 == 0)
                .subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(0);
        sub.assertFailedWith(IllegalArgumentException.class, "request must be positive");
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(test)
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .transform()
                .byTestingItemsWith(
                        x -> Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilterShortcut() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .filter(test)
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithDownstreamRequestingMax() {
        Predicate<Integer> test = x -> x % 2 != 0;
        LongAdder numberOfRequests = new LongAdder();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100000)
                .onRequest().invoke(numberOfRequests::increment)
                .transform().byFilteringItemsWith(test)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertCompleted();
        assertThat(numberOfRequests.longValue()).isEqualTo(1L);
    }

    @Test
    public void testNoEmissionAfterCompletion() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        MultiFilterOp<Integer> filter = new MultiFilterOp<>(processor, x -> x % 2 == 0);
        Subscriber<Integer> subscriber = Mocks.subscriber();
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
        MultiFilterOp<Integer> filter = new MultiFilterOp<>(processor, x -> x % 2 == 0);
        Subscriber<Integer> subscriber = Mocks.subscriber();
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
        AssertSubscriber<Integer> subscriber = processor.transform().byFilteringItemsWith(x -> x % 2 == 0)
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
        AssertSubscriber<Integer> subscriber = processor.transform().byFilteringItemsWith(x -> {
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
