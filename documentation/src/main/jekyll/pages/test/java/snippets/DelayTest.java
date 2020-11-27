package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayTest {

    @Test
    public void testDelayBy() {
        // tag::delay-by[]
        String delayed = Uni.createFrom().item("hello")
                .onItem().delayIt().by(Duration.ofMillis(10))
                .map(s -> "Delayed " + s)
                .await().indefinitely();
        // end::delay-by[]
        assertThat(delayed).isEqualTo("Delayed hello");
    }

    @Test
    public void testDelayUntil() {
        // tag::delay-until[]
        String delayed = Uni.createFrom().item("hello")
                // The write method returns a Uni completed
                // when the operation is done.
                .onItem().delayIt().until(this::write)
                .map(s -> "Written " + s)
                .await().indefinitely();
        // end::delay-until[]
        assertThat(delayed).isEqualTo("Written hello");
    }

    @Test
    public void testDelayMulti() {
        // tag::delay-multi[]
        List<Integer> delayed = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transformToUni(i -> Uni.createFrom().item(i).onItem().delayIt().by(Duration.ofMillis(10)))
                .concatenate()
                .collectItems().asList()
                .await().indefinitely();
        // end::delay-multi[]
        assertThat(delayed).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testDelayMultiRandom() {
        // tag::delay-multi-random[]
        Random random = new Random();
        List<Integer> delayed = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().transformToUni(i -> Uni.createFrom().item(i).onItem().delayIt().by(Duration.ofMillis(random.nextInt(100) + 1)))
                .merge()
                .collectItems().asList()
                .await().indefinitely();
        // end::delay-multi-random[]
        assertThat(delayed).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    private Uni<Void> write(String s) {
        return Uni.createFrom().item(s)
                .onItem().delayIt().by(Duration.ofMillis(20))
                .onItem().ignore().andContinueWithNull();
    }

}
