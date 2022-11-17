package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class MultiToUniTest {

    @Test
    public void testFromEmpty() {
        Uni<Void> uni = Multi.createFrom().<Void> empty().toUni();
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted().assertItem(null);
    }

    @Test
    public void testFromEmpty2() {
        Uni<Void> uni = Uni.createFrom().multi(Multi.createFrom().empty());
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted().assertItem(null);
    }

    @Test
    public void testFromItems() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().items(() -> Stream.of(count.incrementAndGet(), 2, 3, 4));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(1);

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(2);
    }

    @Test
    public void testFromItems2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().items(() -> Stream.of(count.incrementAndGet(), 2, 3, 4));
        Uni<Integer> uni = Uni.createFrom().multi(multi);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(1);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted()
                .assertItem(2);
    }

    @Test
    public void testFromFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom-1");

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testFromFailure2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet()));

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom-1");

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithNoEvents() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().<Void> nothing().onCancellation().invoke(() -> called.set(true));

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertNotTerminated()
                .cancel();

        assertThat(called).isTrue();
    }

    @Test
    public void testWithNoEvents2() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().<Void> nothing().onCancellation().invoke(() -> called.set(true));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertNotTerminated()
                .cancel();

        assertThat(called).isTrue();
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom()
                .completionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(1)
                .assertCompleted();

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(2)
                .assertCompleted();
    }
}
