package io.smallrye.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;

public class MultiThenTest {

    @Test
    public void testChainThen() {
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
                .then(null);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testThatFunctionMustNotThrowException() {
        Multi.createFrom().item(1)
                .then(i -> {
                    throw new IllegalStateException("boom");
                });
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onFailure().retry().indefinitely())
                .then(self -> self.onItem().applyUniAndConcatenate(i -> Uni.createFrom().item(Integer.toString(i))))
                .then(self -> {
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
                .then(self -> self
                        .onItem().apply(i -> i + 1)
                        .onItem().apply(i -> Integer.toString(i)))
                .then(self -> self.collectItems().first())
                .then(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

}
