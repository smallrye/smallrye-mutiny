package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RepetitionsTest {

    @Test
    public void distinct() {
        Multi<Integer> multi = Multi.createFrom().items(1, 1, 2, 3, 4, 5, 5, 6,  1, 4, 4);
        // tag::distinct[]
        List<Integer> list = multi
                .transform().byDroppingDuplicates()
                .collectItems().asList()
                .await().indefinitely();
        // end::distinct[]

        // tag::repetition[]
        List<Integer> list2 = multi
                .transform().byDroppingRepetitions()
                .collectItems().asList()
                .await().indefinitely();
        // end::repetition[]
        assertThat(list).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(list2).containsExactly(1, 2, 3, 4, 5, 6, 1, 4);
    }
}
