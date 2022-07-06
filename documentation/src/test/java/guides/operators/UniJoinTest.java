package guides.operators;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniJoin;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

class UniJoinTest {

    @Test
    void joinAll() {
        // <join-all>
        Uni<Integer> a = Uni.createFrom().item(1);
        Uni<Integer> b = Uni.createFrom().item(2);
        Uni<Integer> c = Uni.createFrom().item(3);

        Uni<List<Integer>> res = Uni.join().all(a, b, c).andCollectFailures();
        // </join-all>

        // <join-all-ff>
        res = Uni.join().all(a, b, c).andFailFast();
        // </join-all-ff>
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
