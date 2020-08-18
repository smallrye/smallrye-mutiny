package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.AssertSubscriber;

public class UniToMultiTest {

    @Test
    public void testFromEmpty() {
        Multi<Void> multi = Uni.createFrom().item((Object) null)
                .onItem().castTo(Void.class)
                .toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty2() {
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().voidItem());
        multi.subscribe().withSubscriber(AssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty3() {
        Multi<Void> multi = Uni.createFrom().voidItem().toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty4() {
        Multi<String> multi = Uni.createFrom().<String> nullItem().toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromEmpty5() {
        Multi<String> multi = Multi.createFrom().uni(Uni.createFrom().nullItem());
        multi.subscribe().withSubscriber(AssertSubscriber.create(1)).assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated().cancel();
    }

    @Test
    public void testFromResult() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom().item(count::incrementAndGet).toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromResult2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom().item(count::incrementAndGet));
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromFailure() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .<Integer> failure(() -> new IOException("boom-" + count.incrementAndGet()))
                .toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testFromFailure2() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Multi.createFrom().uni(Uni.createFrom()
                .failure(() -> new IOException("boom-" + count.incrementAndGet())));
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertHasFailedWith(IOException.class, "boom-1");
        multi.subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertNotTerminated()
                .request(20)
                .assertHasFailedWith(IOException.class, "boom-2");
    }

    @Test
    public void testWithNoEvents() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Uni.createFrom().<Void> nothing()
                .onCancellation().invoke(() -> called.set(true))
                .toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testWithNoEvents2() {
        AtomicBoolean called = new AtomicBoolean();
        Multi<Void> multi = Multi.createFrom().uni(Uni.createFrom().<Void> nothing()
                .onCancellation().invoke(() -> called.set(true)));
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertNotTerminated()
                .cancel()
                .run(() -> assertThat(called).isTrue());
    }

    @Test
    public void testFromAnUniSendingResultEventInTheFuture() {
        AtomicInteger count = new AtomicInteger();
        Multi<Integer> multi = Uni.createFrom()
                .completionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)).toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated()
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
                .completionStage(() -> CompletableFuture.supplyAsync(count::incrementAndGet)));
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .await()
                .assertReceived(1)
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0)).assertNotTerminated()
                .assertHasNotReceivedAnyItem()
                .request(1)
                .await()
                .assertReceived(2)
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture() {
        Multi<Integer> multi = Uni.createFrom()
                .completionStage(() -> CompletableFuture.<Integer> supplyAsync(() -> null)).toMulti();
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }

    @Test
    public void testFromAnUniSendingNullResultEventInTheFuture2() {
        Multi<Integer> multi = Multi.createFrom()
                .uni(Uni.createFrom().completionStage(() -> CompletableFuture.supplyAsync(() -> null)));
        multi.subscribe().withSubscriber(AssertSubscriber.create(1))
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
        multi.subscribe().withSubscriber(AssertSubscriber.create(0))
                .assertNotTerminated()
                .request(1)
                .await()
                .assertHasNotReceivedAnyItem()
                .assertCompletedSuccessfully();
    }
}
