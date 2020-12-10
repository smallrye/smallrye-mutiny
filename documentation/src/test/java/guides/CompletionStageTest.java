package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStageTest {

    @Test
    public void testSubscribeAsCompletionStage() {
        AtomicInteger counter = new AtomicInteger();
        Uni<String> uni = Uni.createFrom().item(() -> "hello-" + counter.getAndIncrement());
        // tag::uni-subscribe-cs[]
        CompletionStage<String> cs = uni.subscribeAsCompletionStage();
        // end::uni-subscribe-cs[]

        assertThat(cs.toCompletableFuture().join()).isEqualTo("hello-0");

        // tag::uni-subscribe-cs-twice[]
        // Trigger the underlying operation twice:
        CompletionStage<String> cs1 = uni.subscribeAsCompletionStage();
        CompletionStage<String> cs2 = uni.subscribeAsCompletionStage();
        // end::uni-subscribe-cs-twice[]
        assertThat(cs1.toCompletableFuture().join()).isEqualTo("hello-1");
        assertThat(cs2.toCompletableFuture().join()).isEqualTo("hello-2");
    }


    @Test
    public void test() {
        Executor executor = Runnable::run;
        // tag::create-uni[]
        Uni<String> uni1 = Uni
                // Create from a Completion Stage
                .createFrom().completionStage(
                        CompletableFuture.supplyAsync(() -> "hello", executor)
                )
                .onItem().transform(String::toUpperCase);

        Uni<String> uni2 = Uni
                // Create from a Completion Stage supplier (recommended)
                .createFrom().completionStage(
                        () -> CompletableFuture.supplyAsync(() -> "hello", executor)
                )
                .onItem().transform(String::toUpperCase);
        // end::create-uni[]

        assertThat(uni1.await().indefinitely()).isEqualTo("HELLO");
        assertThat(uni2.await().indefinitely()).isEqualTo("HELLO");

        // tag::create-multi[]
        Multi<String> multi1 = Multi
                .createFrom().completionStage(
                        CompletableFuture.supplyAsync(() -> "hello", executor)
                )
                .onItem().transform(String::toUpperCase);

        Multi<String> multi2 = Multi
                .createFrom().completionStage(() ->
                        CompletableFuture.supplyAsync(() -> "hello", executor)
                )
                .onItem().transform(String::toUpperCase);
        // end::create-multi[]

        assertThat(multi1.collectItems().asList().await().indefinitely()).containsExactly("HELLO");
        assertThat(multi2.collectItems().asList().await().indefinitely()).containsExactly("HELLO");

    }
}
