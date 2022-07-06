package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplayTest {

    @Test
    public void replayAll() {
        // <replay-all>
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);

        Multi<Integer> replay = Multi.createBy().replaying().ofMulti(upstream);

        List<Integer> items_1 = replay.collect().asList().await().indefinitely();
        List<Integer> items_2 = replay.collect().asList().await().indefinitely();
        // </replay-all>

        assertThat(items_1)
                .isEqualTo(items_2)
                .hasSize(10);
        System.out.println(items_1);
    }

    @Test
    public void replayLast() {
        // <replay-last>
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);

        Multi<Integer> replay = Multi.createBy().replaying().upTo(3).ofMulti(upstream);

        List<Integer> items_1 = replay.collect().asList().await().indefinitely();
        List<Integer> items_2 = replay.collect().asList().await().indefinitely();
        // </replay-last>

        assertThat(items_1)
                .isEqualTo(items_2)
                .hasSize(3);
        System.out.println(items_1);
    }

    @Test
    public void replayWithSeed() {
        // <replay-seed>
        Multi<Integer> upstream = Multi.createFrom().range(0, 10);
        Iterable<Integer> seed = Arrays.asList(-10, -5, -1);

        Multi<Integer> replay = Multi.createBy().replaying().ofSeedAndMulti(seed, upstream);

        List<Integer> items_1 = replay.collect().asList().await().indefinitely();
        List<Integer> items_2 = replay.collect().asList().await().indefinitely();
        // </replay-seed>

        assertThat(items_1)
                .isEqualTo(items_2)
                .hasSize(13);
        System.out.println(items_1);
    }

    @Test
    public void errors() {
        // <replay-errors>
        Multi<Integer> upstream = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(0, 10),
                Multi.createFrom().failure(() -> new IOException("boom"))
        );

        Multi<Integer> replay = Multi.createBy().replaying().upTo(3).ofMulti(upstream);

        replay.subscribe().with(
                n -> System.out.println(" -> " + n),
                failure -> System.out.println("Failed: " + failure.getMessage()),
                () -> System.out.println("Completed"));
        // </replay-errors>
    }
}
