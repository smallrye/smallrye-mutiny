package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class MultiTransformToUniTest {

    @Test
    public void testTransformToUniAndConcatenate() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .concatenate()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndConcatenateWithFailure() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().failure(new RuntimeException("boom")))
                .concatenate()
                .subscribe().withSubscriber(subscriber);

        subscriber.request(1).assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndConcatenateWithNull() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().nullItem())
                .concatenate()
                .subscribe().withSubscriber(subscriber);

        subscriber.request(1).assertHasNotReceivedAnyItem().assertCompleted();
    }

    @Test
    public void testTransformToUniAndConcatenateWithException() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> {
                    throw new RuntimeException("boom");
                })
                .concatenate()
                .subscribe().withSubscriber(subscriber);

        subscriber.request(1).assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndConcatenateWithCancellation() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();
        AtomicBoolean uniCancelled = new AtomicBoolean();

        Multi.createFrom().items(1, 2, 3)
                .onItem()
                .transformToUni(i -> Uni.createFrom()
                        .completionStage(CompletableFuture.supplyAsync(this::delayedHello))
                        .onCancellation().invoke(() -> uniCancelled.set(true)))
                .concatenate()
                .subscribe().withSubscriber(subscriber);

        subscriber.request(1).cancel();
        subscriber.assertNotTerminated().assertHasNotReceivedAnyItem();
        assertThat(uniCancelled.get()).isTrue();
    }

    private String delayedHello() {
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return "hello";
    }

    @Test
    public void testTransformToUniAndConcatenateSingleCall() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUniAndConcatenate(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndMerge() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .merge()
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndMergeWithFailure() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().failure(new RuntimeException("boom")))
                .merge()
                .subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndMergeWithException() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> {
                    throw new RuntimeException("boom");
                })
                .merge()
                .subscribe().withSubscriber(subscriber);

        subscriber.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndMergeWithNull() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().nullItem())
                .merge()
                .subscribe().withSubscriber(subscriber);

        subscriber.assertHasNotReceivedAnyItem().assertCompleted();
    }

    @Test
    public void testTransformToUniAndMergeWithCancellation() {
        AssertSubscriber<Object> subscriber = AssertSubscriber.create();
        AtomicBoolean uniCancelled = new AtomicBoolean();

        Multi.createFrom().items(1, 2, 3)
                .onItem()
                .transformToUni(i -> Uni.createFrom()
                        .completionStage(CompletableFuture.supplyAsync(this::delayedHello))
                        .onCancellation().invoke(() -> uniCancelled.set(true)))
                .merge()
                .subscribe().withSubscriber(subscriber);

        subscriber.request(1).cancel();
        subscriber.assertNotTerminated().assertHasNotReceivedAnyItem();
        assertThat(uniCancelled.get()).isTrue();
    }

    @Test
    public void testTransformToUniAndMergeSingleCall() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUniAndMerge(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndMergeWithUniOfVoid() {
        List<Integer> list = Multi.createFrom().range(1, 6)
                .onItem().transformToUniAndMerge(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                    if (i % 2 == 0) {
                        return null;
                    } else {
                        return i;
                    }
                })))
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactlyInAnyOrder(1, 3, 5);
    }

    @Test
    public void testTransformToUniAndConcatenateWithUniOfVoid() {
        List<Integer> list = Multi.createFrom().range(1, 6)
                .onItem()
                .transformToUniAndConcatenate(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> {
                    if (i % 2 == 0) {
                        return null;
                    } else {
                        return i;
                    }
                })))
                .collect().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 3, 5);
    }
}
