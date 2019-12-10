package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.smallrye.mutiny.Multi;

public class ThenTest {

    @Test
    public void test() {
        // tag::code[]
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .then(self -> {
                    // Transform each item into a string of the item +1
                    return self
                            .onItem().apply(i -> i + 1)
                            .onItem().apply(i -> Integer.toString(i));
                })
                .then(self -> self
                        .onItem().invoke(item -> System.out.println("The item is " + item))
                        .collectItems().first())
                .then(self -> self.await().indefinitely());
        // end::code[]
        assertThat(result).isEqualTo("24");
    }
}
