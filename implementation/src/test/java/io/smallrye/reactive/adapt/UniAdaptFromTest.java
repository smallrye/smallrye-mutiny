package io.smallrye.reactive.adapt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.reactive.Uni;

public class UniAdaptFromTest {

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Uni<Integer> u1 = Uni.createFrom().completionStage(valued);
        Uni<Void> u2 = Uni.createFrom().completionStage(empty);
        Uni<Void> u3 = Uni.createWith(BuiltinConverters.fromCompletionStage(), boom);

        assertThat(u1.await().asOptional().indefinitely()).contains(1);
        assertThat(u2.await().indefinitely()).isEqualTo(null);
        try {
            u3.await();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(Exception.class);
        }
    }

}
