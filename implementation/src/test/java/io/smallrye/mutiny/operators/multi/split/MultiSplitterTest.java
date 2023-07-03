package io.smallrye.mutiny.operators.multi.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class MultiSplitterTest {

    enum OddEven {
        ODD,
        EVEN
    }

    MultiSplitter<Integer, OddEven> evenOddSplitter() {
        return Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .split(OddEven.class, n -> (n % 2 == 0) ? OddEven.EVEN : OddEven.ODD);
    }

    @Test
    void rejectNullKeyType() {
        var err = assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().split(null, null));
        assertThat(err.getMessage()).contains("keyType");
    }

    @Test
    void rejectNullSplitter() {
        var err = assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().nothing().split(OddEven.class, null));
        assertThat(err.getMessage()).contains("splitter");
    }

    @Test
    void rejectNegativeDemand() {
        var sub = evenOddSplitter().get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        sub.request(-10);
        sub.assertFailedWith(IllegalArgumentException.class, "must be greater than 0");
    }

    @Test
    void checkBasicBehavior() {
        var splitter = evenOddSplitter();
        assertThat(splitter.keyType()).isEqualTo(OddEven.class);

        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());

        odd.assertHasNotReceivedAnyItem();
        even.assertHasNotReceivedAnyItem();

        odd.request(2L);

        odd.assertHasNotReceivedAnyItem();
        even.assertHasNotReceivedAnyItem();

        even.request(1L);

        odd.assertItems(1);
        even.assertItems(2);

        even.request(1L);

        odd.assertItems(1, 3);
        even.assertItems(2);

        odd.request(1L);

        odd.assertItems(1, 3);
        even.assertItems(2, 4);

        even.request(1L);

        odd.assertItems(1, 3, 5);
        even.assertItems(2, 4);

        odd.request(Long.MAX_VALUE);

        odd.assertItems(1, 3, 5);
        even.assertItems(2, 4, 6);

        even.request(Long.MAX_VALUE);

        odd.assertItems(1, 3, 5, 7, 9);
        even.assertItems(2, 4, 6, 8, 10);

        odd.assertCompleted();
        even.assertCompleted();

        var afterWork = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        afterWork.assertHasNotReceivedAnyItem().hasCompleted();
    }

    @Test
    void checkPauseOnCancellation() {
        var splitter = evenOddSplitter();
        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());

        odd.request(2L);
        even.request(2L);

        odd.assertItems(1, 3);
        even.assertItems(2);

        even.cancel();
        odd.request(2L);

        odd.assertItems(1, 3);

        even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());
        even.request(2L);

        odd.assertItems(1, 3, 5);
        even.assertItems(4, 6);
    }

    @Test
    void boundedDemandPrevailsOverUnboundedDemand() {
        var splitter = evenOddSplitter();
        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());

        odd.request(Long.MAX_VALUE);
        even.request(2L);

        odd.assertItems(1, 3);
        even.assertItems(2, 4);
    }

    @Test
    void rejectSubscriptionWhenAlreadyActive() {
        var splitter = evenOddSplitter();
        var ok = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        var failing = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        ok.assertHasNotReceivedAnyItem().assertNotTerminated();
        failing.assertFailedWith(IllegalStateException.class, "There is already a subscriber for key ODD");
    }

    @Test
    void nullReturningSplitter() {
        var splitter = Multi.createFrom().items(1, 2, 3)
                .split(OddEven.class, n -> null);
        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());

        odd.request(Long.MAX_VALUE);
        even.request(Long.MAX_VALUE);

        odd.assertFailedWith(NullPointerException.class, "The splitter function returned null");
        even.assertFailedWith(NullPointerException.class, "The splitter function returned null");

        var afterWork = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        afterWork.assertFailedWith(NullPointerException.class, "The splitter function returned null");
    }

    @Test
    void throwingSplitter() {
        var splitter = Multi.createFrom().items(1, 2, 3)
                .split(OddEven.class, n -> {
                    throw new RuntimeException("boom");
                });
        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create());
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create());

        odd.request(Long.MAX_VALUE);
        even.request(Long.MAX_VALUE);

        odd.assertFailedWith(RuntimeException.class, "boom");
        even.assertFailedWith(RuntimeException.class, "boom");

        var afterWork = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        afterWork.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void failedMultiSubscriptions() {
        var splitter = Multi.createFrom().items(1, 2, 3)
                .onCompletion().switchTo(Multi.createFrom().failure(new RuntimeException("boom")))
                .split(OddEven.class, n -> (n % 2 == 0) ? OddEven.EVEN : OddEven.ODD);

        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        odd.assertFailedWith(RuntimeException.class, "boom")
                .assertItems(1, 3);

        even.assertFailedWith(RuntimeException.class, "boom")
                .assertItems(2);

        var afterWork = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        afterWork.assertHasNotReceivedAnyItem()
                .assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    void contextPassing() {
        var splitter = Multi.createFrom().context(ctx -> Multi.createFrom().iterable(ctx.<List<Integer>> get("items")))
                .split(OddEven.class, n -> (n % 2 == 0) ? OddEven.EVEN : OddEven.ODD);

        var ctx = Context.of("items", List.of(1, 2, 3, 4, 5, 6));

        var odd = splitter.get(OddEven.ODD)
                .subscribe().withSubscriber(AssertSubscriber.create(ctx, Long.MAX_VALUE));

        var even = splitter.get(OddEven.EVEN)
                .subscribe().withSubscriber(AssertSubscriber.create(ctx, Long.MAX_VALUE));

        odd.assertCompleted().assertItems(1, 3, 5);
        even.assertCompleted().assertItems(2, 4, 6);
    }
}
