package io.smallrye.mutiny;

import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class UniThenTest {

    @Test
    public void testChainThen() {
        String result = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self ->
                        self
                                .onItem().apply(i -> i + 1)
                                .onFailure().retry().indefinitely()
                )
                .then(self -> self.onItem().produceUni(i -> Uni.createFrom().item(Integer.toString(i))))
                .await().indefinitely();
        assertThat(result).isEqualTo("24");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionMustNotBeNull() {
        Uni.createFrom().item(1)
                .then(null);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*boom.*")
    public void testThatFunctionMustNotThrowException() {
        Uni.createFrom().item(1)
                .then(i -> {
                    throw new IllegalStateException("boom");
                });
    }

    @Test
    public void testThatFunctionCanReturnNullIfVoid() {
        AtomicReference<String> result = new AtomicReference<>();
        Void x = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self ->
                        self
                                .onItem().apply(i -> i + 1)
                                .onFailure().retry().indefinitely()
                )
                .then(self -> self.onItem().produceUni(i -> Uni.createFrom().item(Integer.toString(i))))
                .then(self -> {
                    String r = self.await().indefinitely();
                    result.set(r);
                    return null; // void
                });
        assertThat(result).hasValue("24");
        assertThat(x).isNull();
    }

    @Test
    public void testChainingMulti() {
        String result = Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self ->
                    Multi.createFrom().uni(self)
                )
                .then(self ->
                        self
                                .onItem().apply(i -> i + 1)
                                .onItem().apply(i -> Integer.toString(i)))
                .then(self -> self.collectItems().first())
                .then(self -> self.await().indefinitely());
        assertThat(result).isEqualTo("24");
    }

}
