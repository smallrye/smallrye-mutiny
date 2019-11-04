package io.smallrye.reactive.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.multi.BuiltinConverters;
import io.smallrye.reactive.groups.MultiCollect;

public class MultiCreateFromTest {

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Multi<Integer> m1 = Multi.createFrom().completionStage(valued);
        Multi<Void> m2 = Multi.createFrom().completionStage(empty);
        Multi<Void> m3 = Multi.createFrom().converter(BuiltinConverters.fromCompletionStage(), boom);

        MultiCollect<Integer> mList = m1.collect();
        assertThat(mList.first().await().indefinitely()).isEqualTo(1);
        assertThat(mList.last().await().indefinitely()).isEqualTo(1);
        assertThat(m2.collect().first().await().indefinitely()).isEqualTo(null);
        try {
            m3.collect();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(Exception.class);
        }
    }
}
