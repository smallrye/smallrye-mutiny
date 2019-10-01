package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.Uni;

public class MultiToUniTest {

    @Test
    public void testFromEmpty() {
        Uni<Void> uni = Multi.createFrom().<Void> empty().toUni();
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testFromEmpty2() {
        Uni<Void> uni = Uni.createFrom().multi(Multi.createFrom().empty());
        uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testFromItems() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().deferredItems(() -> Stream.of(count.incrementAndGet(), 2, 3, 4));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertItem(1);

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertItem(2);
    }

    @Test
    public void testFromItems2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().deferredItems(() -> Stream.of(count.incrementAndGet(), 2, 3, 4));
        Uni<Integer> uni = Uni.createFrom().multi(multi);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertItem(1);
        uni.subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompletedSuccessfully()
                .assertItem(2);
    }

    @Test
    public void testFromFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom()
                .deferredFailure(() -> new IOException("boom-" + count.incrementAndGet()));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailure(IOException.class, "boom-1");

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailure(IOException.class, "boom-2");
    }

    @Test
    public void testFromFailure2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom()
                .deferredFailure(() -> new IOException("boom-" + count.incrementAndGet()));

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailure(IOException.class, "boom-1");

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailure(IOException.class, "boom-2");
    }

    @Test
    public void testWithNoEvents() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().<Void> nothing().on().cancellation(() -> called.set(true));

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertNoSignals()
                .cancel();

        assertThat(called).isTrue();
    }

    @Test
    public void testWithNoEvents2() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().<Void> nothing().on().cancellation(() -> called.set(true));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertNoSignals()
                .cancel();

        assertThat(called).isTrue();
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture() {
        AtomicInteger count = new AtomicInteger();

        Multi<Integer> multi = Multi.createFrom()
                .deferredCompletionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet));

        multi.toUni().subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(1)
                .assertCompletedSuccessfully();

        Uni.createFrom().multi(multi).subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(2)
                .assertCompletedSuccessfully();
    }
}
