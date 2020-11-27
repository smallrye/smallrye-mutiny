package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class HowToChainAsyncTest {

    @Test
    public void test() {
        Uni<String> uni = Uni.createFrom().item("hello");

        // tag::code[]

        CompletableFuture<String> future = uni
                .onItem().transformToUni(this::asyncOperation)
                .onItem().transformToUni(this::anotherAsyncOperation)
                .subscribeAsCompletionStage();

        // end::code[]
        assertThat(future.join()).isEqualTo("HELLO!");
    }

    public Uni<String> asyncOperation(String param) {
        return Uni.createFrom().completionStage(CompletableFuture.supplyAsync(param::toUpperCase));
    }

    public Uni<String> anotherAsyncOperation(String param) {
        return Uni.createFrom().completionStage(CompletableFuture.supplyAsync(() -> param + "!"));
    }
}
