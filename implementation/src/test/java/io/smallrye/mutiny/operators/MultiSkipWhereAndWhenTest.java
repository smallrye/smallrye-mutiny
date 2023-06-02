package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

public class MultiSkipWhereAndWhenTest {

    @Test
    public void testThatPredicateCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .skip().where(null));
    }

    @Test
    public void testThatFunctionCannotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 4)
                .skip().when(null));
    }

    @Test
    public void cannotRequestZeroItems() {
        AssertSubscriber<Integer> sub = Multi.createFrom().range(1, 4)
                .skip().where(n -> n % 2 == 0)
                .subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(0);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .skip().where(test).collect().asList()
                .await().indefinitely()).containsExactly(2);
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .skip().when(
                        x -> Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collect().asList()
                .await().indefinitely()).containsExactly(2);
    }

    @Test
    public void testFilteringWithDownstreamRequestingMax() {
        Predicate<Integer> test = x -> x % 2 != 0;
        LongAdder numberOfRequests = new LongAdder();
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 100000)
                .onRequest().invoke(numberOfRequests::increment)
                .skip().where(test)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertCompleted();
        assertThat(numberOfRequests.longValue()).isEqualTo(1L);
    }

    @Test
    public void testNoEmissionAfterCancellation() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> subscriber = processor.skip().where(x -> x % 2 == 0)
                .subscribe().withSubscriber(AssertSubscriber.create(20));

        processor.onNext(1);
        processor.onNext(2);
        processor.onNext(3);
        processor.onNext(4);
        subscriber.cancel();
        processor.onNext(5);
        processor.onNext(6);
        processor.onComplete();
        subscriber.assertItems(1, 3)
                .assertNotTerminated();
    }

    @Test
    public void testWithPredicateThrowingException() {
        BroadcastProcessor<Integer> processor = BroadcastProcessor.create();
        AssertSubscriber<Integer> subscriber = processor.skip().where(x -> {
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
        subscriber.assertItems(1)
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }
}
