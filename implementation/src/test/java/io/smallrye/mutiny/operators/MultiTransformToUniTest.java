package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

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
    public void testTransformToUniAndMerge() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .merge()
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactlyInAnyOrder(2, 3, 4);
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

    @Test
    public void testApplyUniAndMerge() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUniAndMerge(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactlyInAnyOrder(2, 3, 4);
    }

    @Test
    public void testApplyUniAndConcatenate() {
        List<Integer> list = Multi.createFrom().range(1, 4)
                .onItem()
                .transformToUniAndConcatenate(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i + 1)))
                .collectItems().asList().await().indefinitely();

        assertThat(list).containsExactly(2, 3, 4);
    }

    @Test
    public void testApplyUniAndMergeWithUniOfVoid() {
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
    public void testApplyUniAndConcatenateWithUniOfVoid() {
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

}
