package guides.operators;

import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class UniJoinTest {

    @Test
    void joinAll() {
        // <join-all-ff>
        Uni<Integer> a = Uni.createFrom().item(1);
        Uni<Integer> b = Uni.createFrom().item(2);
        Uni<Integer> c = Uni.createFrom().item(3);

        Uni<List<Integer>> res = Uni.join().all(a, b, c).andFailFast();
        // </join-all-ff>

        assertThat(res.await().atMost(Duration.ofSeconds(3)))
            .containsExactly(1, 2, 3);
    }

    @Test
    void joinAllEmpty() {
        // <join-all-ff>
        Uni<List<Object>> res = Uni.join().all().andFailFast();
        // </join-all-ff>

        assertThat(res.await().atMost(Duration.ofSeconds(3)))
            .isEmpty();
    }

    void joinFirst(Uni<Integer> a, Uni<Integer> b, Uni<Integer> c) {

        // <join-first>
        Uni<Integer> res = Uni.join().first(a, b, c).toTerminate();
        // </join-first>

        // <join-first-withitem>
        res = Uni.join().first(a, b, c).withItem();
        // </join-first-withitem>

        Supplier<Uni<Integer>> supplier = () -> Uni.createFrom().item(63);
        boolean someCondition = false;

        // <builder>
        UniJoin.Builder<Integer> builder = Uni.join().builder();

        while (someCondition) {
            Uni<Integer> uni = supplier.get();
            builder.add(uni);
        }

        Uni<List<Integer>> all = builder.joinAll().andFailFast();

        Uni<Integer> first = builder.joinFirst().withItem();
        // </builder>
    }
}
