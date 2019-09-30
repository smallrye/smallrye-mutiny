package io.smallrye.reactive.unimulti.operators;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class UniToMultiTest {

    @Test
    public void testFromEmpty() {
        Multi<Void> multi = Uni.createFrom().item(null)
                .onItem().castTo(Void.class)
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty2() {
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().item(null));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromResult() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom().deferredItem(count::incrementAndGet).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromResult2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom().deferredItem(count::incrementAndGet));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .<Integer>deferredFailure(() -> new IOException("boom-" + count.incrementAndGet()))
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testFromFailure2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom()
                .deferredFailure(() -> new IOException("boom-" + count.incrementAndGet())));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithNoEvents() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Uni.createFrom().<Void>nothing()
                .on().cancellation(() -> called.set(true))
                .toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testWithNoEvents2() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().<Void>nothing()
                .on().cancellation(() -> called.set(true)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .deferredCompletionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .await()
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom()
                .deferredCompletionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .await()
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture() {
        Multi<Integer> multi = Uni.createFrom()
                .deferredCompletionStage(() -> CompletableFuture.<Integer>supplyAsync(() -> null)).toMulti();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture2() {
        Multi<Integer> multi = Multi.createFrom()
                .uni(Uni.createFrom().deferredCompletionStage(() -> CompletableFuture.supplyAsync(() -> null)));
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(MultiAssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }
}
