package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class CompletionStageTest {

    @Test
    public void test() {
        // tag::code[]
        CompletableFuture<String> future1 = Uni
                // Create from a Completion Stage
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"))
                .map(String::toUpperCase)
                .subscribeAsCompletionStage(); // Retrieve as a Completion Stage

        CompletableFuture<List<String>> future2 = Multi
                .createFrom().completionStage(CompletableFuture.supplyAsync(() -> "hello"))
                .map(String::toUpperCase)
                .collectItems().asList() // Accumulate items in a list (return a Uni<List<T>>)
                .subscribeAsCompletionStage();// Retrieve the list as a Completion Stage

        // end::code[]
        assertThat(future1.join()).isEqualTo("HELLO");
        assertThat(future2.join()).containsExactly("HELLO");
    }
}
