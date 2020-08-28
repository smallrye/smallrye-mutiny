package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.BuiltinConverters;

public class UniConvertToTest {

    @Test
    public void testCreatingCompletionStages() {
        Uni<Integer> valued = Uni.createFrom().item(1);
        Uni<Void> empty = Uni.createFrom().voidItem();
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletionStage<Integer> stage1 = valued.convert().toCompletionStage();
        CompletionStage<Void> stage2 = empty.convert().with(BuiltinConverters.toCompletionStage());
        CompletionStage<Void> stage3 = failure.convert().toCompletionStage();

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }

    @Test
    public void testCreatingCompletableFutures() {
        Uni<Integer> valued = Uni.createFrom().item(1);
        Uni<Void> empty = Uni.createFrom().voidItem();
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletableFuture<Integer> stage1 = valued.convert().toCompletableFuture();
        CompletableFuture<Void> stage2 = empty.convert().toCompletableFuture();
        CompletableFuture<Void> stage3 = failure.convert().with(BuiltinConverters.toCompletableFuture());

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }
}
