package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

public class MultiThenTest {

    @Test
    public void testChainStage() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .then(self -> self.onItem().produceCompletionStage(i -> CompletableFuture.supplyAsync(() -> i)).concatenate())
                .then(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(m -> m.onItem().transform(i -> Integer.toString(i)))
                .then(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test
    public void testChainThenWithDeprecatedApply() {
        List<String> result = Multi.createFrom().items(1, 2, 3)
                .then(self -> self.onItem().produceCompletionStage(i -> CompletableFuture.supplyAsync(() -> i)).concatenate())
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(m -> m.onItem().apply(i -> Integer.toString(i)))
                .then(m -> m.collectItems().asList())
                .await().indefinitely();
        assertThat(result).containsExactly("2", "3", "4");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionMustNotBeNull() {
        Multi.createFrom().item(1)
                .stage(null);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testThatFunctionMustNotThrowException() {
        Multi.createFrom().item(1)
                .stage(i -> {
                    throw new IllegalStateException("boom");
                });
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> self
                        .onItem().transform(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .stage(self -> self.onItem().produceUni(i -> Uni.createFrom().item(Integer.toString(i))).concatenate())
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
