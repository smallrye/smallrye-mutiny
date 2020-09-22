package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

public class StageTest {

    @Test
    public void test() {
        // tag::code[]
        String result = Multi.createFrom().completionStage(CompletableFuture.supplyAsync(() -> 23))
                .stage(self -> {
                    // Transform each item into a string of the item +1
                    return self
                            .onItem().transform(i -> i + 1)
                            .onItem().transform(i -> Integer.toString(i));
                })
                .stage(self -> self
                        .onItem().invoke(item -> System.out.println("The item is " + item))
                        .collectItems().first())
                .stage(self -> self.await().indefinitely());
        // end::code[]
        assertThat(result).isEqualTo("24");
    }
}
