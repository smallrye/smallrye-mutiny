package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetitionsTest {

    @Test
    public void distinct() {
        Multi<Integer> multi = Multi.createFrom().items(1, 1, 2, 3, 4, 5, 5, 6,  1, 4, 4);
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
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6, 1, 4);
    }
}
