package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.BuiltinConverters;

public class UniCreateFromTest {

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Uni<Integer> u1 = Uni.createFrom().completionStage(valued);
        Uni<Void> u2 = Uni.createFrom().completionStage(empty);
        Uni<Void> u3 = Uni.createFrom().converter(BuiltinConverters.fromCompletionStage(), boom);

        assertThat(u1.await().asOptional().indefinitely()).contains(1);
        assertThat(u2.await().indefinitely()).isEqualTo(null);
        assertThatThrownBy(() -> u3.await().indefinitely())
                .isInstanceOf(CompletionException.class).hasMessageContaining("boom");
    }

    @Test
    public void testCreationFromSupplier() {
        assertThatThrownBy(() -> Uni.createFrom().item(null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplier");

        assertThat(Uni.createFrom().item(() -> null)
                .await().indefinitely()).isNull();

        assertThat(Uni.createFrom().item(() -> 1)
                .await().indefinitely()).isEqualTo(1);

        assertThatThrownBy(() -> Uni.createFrom().item(() -> {
            throw new IllegalStateException("boom");
        })
                .await().indefinitely()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().item(counter::incrementAndGet);
        assertThat(uni.await().indefinitely()).isEqualTo(1);
        assertThat(uni.await().indefinitely()).isEqualTo(2);
        assertThat(uni.await().indefinitely()).isEqualTo(3);
    }

    @Test
    public void testCreationFromFailureSupplier() {
        assertThatThrownBy(() -> Uni.createFrom().failure((Supplier<Throwable>) null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("supplier");

        assertThatThrownBy(() -> Uni.createFrom().failure(() -> null).await().indefinitely())
                .isInstanceOf(NullPointerException.class).hasMessageContaining("supplier");

        assertThatThrownBy(() -> Uni.createFrom().failure(() -> new IllegalStateException("boom")).await().indefinitely())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom");

        assertThatThrownBy(() -> Uni.createFrom().failure(() -> new IOException("boom")).await().indefinitely())
                .isInstanceOf(CompletionException.class).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");

        assertThatThrownBy(() -> Uni.createFrom().failure(() -> {
            throw new IllegalStateException("boom");
        })
                .await().indefinitely()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        AtomicInteger counter = new AtomicInteger();
        Uni<Integer> uni = Uni.createFrom().failure(() -> new IllegalStateException("boom-" + counter.incrementAndGet()));
        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom-1");
        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom-2");
        assertThatThrownBy(() -> uni.await().indefinitely())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("boom-3");
    }

}
