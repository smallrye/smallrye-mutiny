package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.smallrye.mutiny.unchecked.Unchecked.function;
import static org.assertj.core.api.Assertions.assertThat;

public class HowToTransformTest {

    @Test
    public void transformSync() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::sync[]
        String result1 = uni
                .onItem().transform(s -> s.toUpperCase())
                .await().indefinitely();
        List<String> result2 = multi
                .onItem().transform(String::toUpperCase)
                .collectItems().asList().await().indefinitely();
        // end::sync[]

        assertThat(result1).isEqualTo("HELLO");
        assertThat(result2).containsExactly("HELLO", "WORLD");
    }

    private String operationThrowingException(String s) throws IOException {
        return s.toUpperCase();
    }

    @Test
    public void transformSyncUnchecked() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::sync-unchecked[]
        String result1 = uni
                .onItem().transform(function(this::operationThrowingException))
                .await().indefinitely();
        List<String> result2 = multi
                .onItem().transform(function(this::operationThrowingException))
                .collectItems().asList().await().indefinitely();
        // end::sync-unchecked[]

        assertThat(result1).isEqualTo("HELLO");
        assertThat(result2).containsExactly("HELLO", "WORLD");
    }

    @Test
    public void transformAsync() {
        Uni<String> uni = Uni.createFrom().item("hello");
        Multi<String> multi = Multi.createFrom().items("hello", "world");

        // tag::async[]
        String result1 = uni
                .onItem().transformToUni(s -> Uni.createFrom().item(s.toUpperCase()))
                .await().indefinitely();
        String result2 = uni
                .onItem().transformToUni(s -> Uni.createFrom().completionStage(
                        CompletableFuture.supplyAsync(() -> s.toUpperCase()))
                )
                .await().indefinitely();
        List<String> result3 = multi
                .onItem().transformToUniAndMerge(s -> Uni.createFrom().item(s.toUpperCase()))
                .collectItems().asList().await().indefinitely();
        List<String> result4 = multi
                .onItem().transformToUniAndConcatenate(s -> Uni.createFrom().item(s.toUpperCase()))
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
                .onItem().transformToMultiAndConcatenate(s -> Multi.createFrom().item(s.toUpperCase()))
                .collectItems().asList().await().indefinitely();
        // end::multi[]

        assertThat(result).containsExactly("HELLO", "WORLD");
    }
}
