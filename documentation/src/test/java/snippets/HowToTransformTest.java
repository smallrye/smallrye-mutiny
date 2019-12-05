package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class HowToTransformTest {

    @Test
    public void transformSync() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::sync[]
        String result1 = uni
                .onItem().mapToItem(s -> s.toUpperCase())
                .await().indefinitely();
        List<String> result2 = multi
                .onItem().mapToItem(s -> s.toUpperCase())
                .collectItems().asList().await().indefinitely();
        // end::sync[]

        assertThat(result1).isEqualTo("HELLO");
        assertThat(result2).containsExactly("HELLO", "WORLD");
    }

    @Test
    public void transformAsync() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::async[]
        String result1 = uni
                .onItem().mapToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely();
        String result2 = uni
                .onItem().mapToCompletionStage(s -> CompletableFuture.supplyAsync(() -> s.toUpperCase()))
                .await().indefinitely();
        List<String> result3 = multi
                .onItem().flatMap().uni(s -> Uni.createFrom().item(s.toUpperCase())).concatenateResults()
                .collectItems().asList().await().indefinitely();
        List<String> result4 = multi
                .onItem().flatMap().completionStage(s -> CompletableFuture.supplyAsync(() -> s.toUpperCase()))
                .concatenateResults()
                .collectItems().asList().await().indefinitely();
        // end::async[]

        assertThat(result1).isEqualTo("HELLO");
        assertThat(result2).isEqualTo("HELLO");
        assertThat(result3).containsExactly("HELLO", "WORLD");
        assertThat(result4).containsExactly("HELLO", "WORLD");
    }

    @Test
    public void transformMulti() {
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::multi[]
        List<String> result = multi
                .onItem().flatMap().publisher(s -> Multi.createFrom().item(s.toUpperCase())).concatenateResults()
                .collectItems().asList().await().indefinitely();
        // end::multi[]

        assertThat(result).containsExactly("HELLO", "WORLD");
    }
}
