package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiTransformToUniTest {

    @Test
    public void testTransformToUniAndConcatenate() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .concatenate()
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndConcatenateWithFailure() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().failure(new RuntimeException("boom")))
                .concatenate()
                .subscribe().withSubscriber(ts);

        ts.assertHasFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndConcatenateWithNull() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().nullItem())
                .concatenate()
                .subscribe().withSubscriber(ts);

        ts.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testTransformToUniAndConcatenateWithException() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> {
                    throw new RuntimeException("boom");
                })
                .concatenate()
                .subscribe().withSubscriber(ts);

        ts.assertHasFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndConcatenateWithCancellation() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();
        AtomicBoolean uniCancelled = new AtomicBoolean();

        Multi.createFrom().items(1, 2, 3)
                .onItem()
                .transformToUni(i -> Uni.createFrom()
                        .completionStage(CompletableFuture.supplyAsync(this::delayedHello))
                        .onCancellation().invoke(() -> uniCancelled.set(true)))
                .concatenate()
                .subscribe().withSubscriber(ts);

        ts.request(1).cancel();
        ts.assertHasNotCompleted().assertHasNotFailed().assertHasNotReceivedAnyItem();
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
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndMerge() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    public void testTransformToUniAndMergeWithFailure() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().failure(new RuntimeException("boom")))
                .merge()
                .subscribe().withSubscriber(ts);

        ts.assertHasFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndMergeWithException() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> {
                    throw new RuntimeException("boom");
                })
                .merge()
                .subscribe().withSubscriber(ts);

        ts.assertHasFailedWith(RuntimeException.class, "boom");
    }

    @Test
    public void testTransformToUniAndMergeWithNull() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();

        Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().nullItem())
                .merge()
                .subscribe().withSubscriber(ts);

        ts.assertHasNotReceivedAnyItem().assertCompletedSuccessfully();
    }

    @Test
    public void testTransformToUniAndMergeWithCancellation() {
        MultiAssertSubscriber<Object> ts = MultiAssertSubscriber.create();
        AtomicBoolean uniCancelled = new AtomicBoolean();

        Multi.createFrom().items(1, 2, 3)
                .onItem()
                .transformToUni(i -> Uni.createFrom()
                        .completionStage(CompletableFuture.supplyAsync(this::delayedHello))
                        .onCancellation().invoke(() -> uniCancelled.set(true)))
                .merge()
                .subscribe().withSubscriber(ts);

        ts.request(1).cancel();
        ts.assertHasNotCompleted().assertHasNotFailed().assertHasNotReceivedAnyItem();
        assertThat(uniCancelled.get()).isTrue();
    }

    @Test
    public void testTransformToUniAndMergeSingleCall() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUniAndMerge(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .collectItems().asList().await().indefinitely();
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
                .collectItems().asList().await().indefinitely();

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
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(1, 3, 5);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProduceUniDeprecated() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem().produceUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .concatenate()
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }
}
