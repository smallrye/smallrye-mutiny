package guides;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RxTest {

    @Test
    public void rx() {
        Multi<Integer> multi = Multi.createFrom().range(1, 3);
        Uni<Integer> uni = Uni.createFrom().item(1);
        // <rx>

        int result = uni
                .map(i -> i + 1)
                .await().indefinitely();

        int result2 = uni
                .flatMap(i -> Uni.createFrom().item(i + 1))
                .await().indefinitely();

        List<Integer> list = multi
                .map(i -> i + 1)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .flatMap(i -> Multi.createFrom().items(i, i))
                .collect().asList()
                .await().indefinitely();

        List<Integer> list3 = multi
                .concatMap(i -> Multi.createFrom().items(i, i))
                .collect().asList()
                .await().indefinitely();

        // </rx>
        assertThat(result).isEqualTo(2);
        assertThat(result2).isEqualTo(2);
        assertThat(list).containsExactly(2, 3);
        assertThat(list2).containsExactly(1, 1, 2, 2);
        assertThat(list3).containsExactly(1, 1, 2, 2);
    }

    @Test
    public void mutiny() {
        Multi<Integer> multi = Multi.createFrom().range(1, 3);
        Uni<Integer> uni = Uni.createFrom().item(1);
        // <mutiny>

        int result = uni
                .onItem().transform(i -> i + 1)
                .await().indefinitely();

        int result2 = uni
                .onItem().transformToUni(i -> Uni.createFrom().item(i + 1))
                .await().indefinitely();

        // Shortcut for .onItem().transformToUni
        int result3 = uni
                .chain(i -> Uni.createFrom().item(i + 1))
                .await().indefinitely();

        List<Integer> list = multi
                .onItem().transform(i -> i + 1)
                .collect().asList()
                .await().indefinitely();

        List<Integer> list2 = multi
                .onItem().transformToMultiAndMerge(i -> Multi.createFrom().items(i, i))
                .collect().asList()
                .await().indefinitely();

        // Equivalent to transformToMultiAndMerge but let you configure the flattening process,
        // failure management, concurrency...
        List<Integer> list3 = multi
                .onItem().transformToMulti(i -> Multi.createFrom().items(i, i)).merge()
                .collect().asList()
                .await().indefinitely();

        List<Integer> list4 = multi
                .onItem().transformToMultiAndConcatenate(i -> Multi.createFrom().items(i, i))
                .collect().asList()
                .await().indefinitely();

        // Equivalent to transformToMultiAndConcatenate but let you configure the flattening process,
        // failure management...
        List<Integer> list5 = multi
                .onItem().transformToMulti(i -> Multi.createFrom().items(i, i)).concatenate()
                .collect().asList()
                .await().indefinitely();


        // </mutiny>
        assertThat(result).isEqualTo(2);
        assertThat(result2).isEqualTo(2);
        assertThat(result3).isEqualTo(2);
        assertThat(list).containsExactly(2, 3);
        assertThat(list2).containsExactly(1, 1, 2, 2);
        assertThat(list3).containsExactly(1, 1, 2, 2);
        assertThat(list4).containsExactly(1, 1, 2, 2);
        assertThat(list5).containsExactly(1, 1, 2, 2);
    }


}
