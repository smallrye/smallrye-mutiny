package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class MergeVsConcatenateTest {

    Random random = new Random();

    public Uni<String> asyncService(String input) {
        int delay = random.nextInt(100) + 1;
        return Uni.createFrom().item(input::toUpperCase)
                .onItem().delayIt().by(Duration.ofMillis(delay));
    }

    Multi<String> multi = Multi.createFrom().items("a", "b", "c", "d", "e", "f", "g", "h");

    @Test
    public void testConcatenate() {
        // tag::concatenate[]
        List<String> list = multi
                .onItem().transformToUniAndConcatenate(this::asyncService)
                .collectItems().asList()
                .await().indefinitely();
        // end::concatenate[]
        assertThat(list).containsExactly("A", "B", "C", "D", "E", "F", "G", "H");
    }

    @Test
    public void testMerge() {
        // tag::merge[]
        List<String> list = multi
                .onItem().transformToUniAndMerge(this::asyncService)
                .collectItems().asList()
                .await().indefinitely();
        // end::merge[]
        assertThat(list).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
    }

    @Test
    public void testMergeWithConcurrency() {
        // tag::merge-concurrency[]
        List<String> list = multi
                .onItem().transformToUni(this::asyncService).merge(8)
                .collectItems().asList()
                .await().indefinitely();
        // end::merge-concurrency[]
        assertThat(list).containsExactlyInAnyOrder("A", "B", "C", "D", "E", "F", "G", "H");
    }

}
