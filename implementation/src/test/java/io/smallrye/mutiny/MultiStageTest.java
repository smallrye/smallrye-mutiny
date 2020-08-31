package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class MultiStageTest {

    @Test
    public void testChainStage() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .stage(self -> self.onItem()
                        .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                        .concatenate())
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(m -> m.onItem().transform(i -> Integer.toString(i)))
                .stage(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testChainWithDeprecatedThenAndApply() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .then(self -> self.onItem()
                        .transformToUni(i -> Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> i)))
                        .concatenate())
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(m -> m.onItem().apply(i -> Integer.toString(i)))
                .then(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test
    public void testThatFunctionMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().item(1)
                .stage(null));
    }

    @Test
    public void testThatFunctionMustNotThrowException() {
        assertThrows(IllegalStateException.class, () -> Multi.createFrom().item(1)
                .stage(i -> {
                    throw new IllegalStateException("boom");
                }));
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(self -> self.onItem().transformToUni(i -> Uni.createFrom().item(Integer.toString(i)))
                        .concatenate())
                .stage(self -> {
                    String r = self.collectItems().first().await().indefinitely();
                    result.set(r);
                    return null; // void
                });
        assertThat(result).hasValue("24");
        assertThat(x).isNull();
    }

    @Test
    public void testChainingUni() {
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onItem().transform(i -> Integer.toString(i)))
                .stage(self -> self.collectItems().first())
                .stage(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

    @Test
    public void testChainingUniWithDeprecatedApplyAndThen() {
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onItem().apply(i -> Integer.toString(i)))
                .then(self -> self.collectItems().first())
                .then(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

}
