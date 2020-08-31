package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

public class BackPressureTest {

    @Test
    public void test() {
        Multi<String> multi = Multi.createFrom().items("a", "b", "c");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        // tag::code[]

        String res1 = multi
                .emitOn(executor)
                .onOverflow().buffer(10)
                .collectItems().first()
                .await().indefinitely();

        String res2 = multi
                .emitOn(executor)
                .onOverflow().dropPreviousItems()
                .collectItems().first()
                .await().indefinitely();

        // end::code[]
        assertThat(res1).isEqualTo("a");
        assertThat(res2).isEqualTo("a");
    }
}
