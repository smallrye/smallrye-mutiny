package guides.operators;

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
        Uni<String> delayed = Uni.createFrom().item("hello")
                .onItem().delayIt().by(Duration.ofMillis(10));
        // end::delay-by[]
        String r = delayed.map(s -> "Delayed " + s)
                .await().indefinitely();
        assertThat(r).isEqualTo("Delayed hello");
    }

    @Test
    public void testDelayUntil() {
        // tag::delay-until[]
        Uni<String> delayed = Uni.createFrom().item("hello")
                // The write method returns a Uni completed
                // when the operation is done.
                .onItem().delayIt().until(this::write);
        // end::delay-until[]
        String r = delayed
                .map(s -> "Written " + s)
                .await().indefinitely();
        assertThat(r).isEqualTo("Written hello");
    }

    @Test
    public void testDelayMulti() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5);
        // tag::delay-multi[]
        Multi<Integer> delayed = multi
            .onItem().call(i ->
                // Delay the emission until the returned uni emits its item
                Uni.createFrom().nullItem().onItem().delayIt().by(Duration.ofMillis(10))
            );
        // end::delay-multi[]
        assertThat(delayed.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testThrottling() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5);
        // tag::throttling-multi[]
        // Introduce a one second delay between each item
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop();
        Multi<Integer> delayed = Multi.createBy().combining().streams(ticks, multi)
                .using((x, item) -> item);
        // end::throttling-multi[]
        assertThat(delayed.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testDelayMultiRandom() {
        // tag::delay-multi-random[]
        Random random = new Random();
        Multi<Integer> delayed = Multi.createFrom().items(1, 2, 3, 4, 5)
                .onItem().call(i -> {
                    Duration delay = Duration.ofMillis(random.nextInt(100) + 1);
                    return Uni.createFrom().nullItem().onItem().delayIt().by(delay);
                });
        // end::delay-multi-random[]
        assertThat(delayed.collectItems().asList()
                .await().indefinitely())
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    private Uni<Void> write(String s) {
        return Uni.createFrom().item(s)
                .onItem().delayIt().by(Duration.ofMillis(20))
                .onItem().ignore().andContinueWithNull();
    }

}
