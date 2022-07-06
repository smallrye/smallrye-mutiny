package guides.operators;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterTest {

    @Test
    public void filter() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // <filter>
        List<Integer> list = multi
                .select().where(i -> i > 6)
                .collect().asList()
                .await().indefinitely();
        // </filter>

        // <test>
        List<Integer> list2 = multi
                .select().when(i -> Uni.createFrom().item(i > 6))
                .collect().asList()
                .await().indefinitely();
        // </test>

        assertThat(list).containsExactly(7, 8, 9, 10);
        assertThat(list2).containsExactly(7, 8, 9, 10);
    }

    @Test
    public void take() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // <take>
        List<Integer> list = multi
                .select().first(2)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .select().first(i -> i < 3)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list3 = multi
                .select().last(2)
                .collect().asList()
                .await().indefinitely();
        // </take>
        assertThat(list).containsExactly(1, 2);
        assertThat(list2).containsExactly(1, 2);
        assertThat(list3).containsExactly(9, 10);
    }

    @Test
    public void skip() {
        Multi<Integer> multi = Multi.createFrom().range(1, 11);
        // <skip>
        List<Integer> list = multi
                .skip().first(8)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .skip().first(i -> i < 9)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list3 = multi
                .skip().last(8)
                .collect().asList()
                .await().indefinitely();
        // </skip>
        assertThat(list).containsExactly(9, 10);
        assertThat(list2).containsExactly(9, 10);
        assertThat(list3).containsExactly(1, 2);
    }

    @Test
    public void distinct() {
        Multi<Integer> multi = Multi.createFrom().items(1, 1, 2, 3, 4, 5, 5, 6);
        // <distinct>
        List<Integer> list = multi
                .select().distinct()
                .collect().asList()
                .await().indefinitely();
        // </distinct>

        // <repetition>
        List<Integer> list2 = multi
                .skip().repetitions()
                .collect().asList()
                .await().indefinitely();
        // </repetition>
        assertThat(list).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6);
    }
}
