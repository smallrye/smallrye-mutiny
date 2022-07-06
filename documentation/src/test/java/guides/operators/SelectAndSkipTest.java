package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectAndSkipTest {

    @Test
    public void testSelect() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9);

        // <take-first>
        Multi<Integer> firstThreeItems = multi.select().first(3);
        // </take-first>

        // <take-last>
        Multi<Integer> lastThreeItems = multi.select().last(3);
        // </take-last>

        // <take-while>
        Multi<Integer> takeWhile = multi.select().first(i -> i < 4);
        // </take-while>

        // <take-for>
        Multi<Integer> takeForDuration = multi.select().first(Duration.ofSeconds(1));
        // </take-for>

        assertThat(firstThreeItems.collect().asList().await().indefinitely()).containsExactly(1, 2, 3);
        assertThat(takeWhile.collect().asList().await().indefinitely()).containsExactly(1, 2, 3);
        assertThat(lastThreeItems.collect().asList().await().indefinitely()).containsExactly(7, 8, 9);
        assertThat(takeForDuration.collect().asList().await().indefinitely()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    }

    @Test
    public void testSkip() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9);

        // <skip-first>
        Multi<Integer> skipThreeItems = multi.skip().first(3);
        // </skip-first>

        // <skip-last>
        Multi<Integer>  skipLastThreeItems = multi.skip().last(3);
        // </skip-last>

        // <skip-while>
        Multi<Integer> skipWhile = multi.skip().first(i -> i < 4);
        // </skip-while>

        // <skip-for>
        Multi<Integer> skipForDuration = multi.skip().first(Duration.ofSeconds(1));
        // </skip-for>

        assertThat(skipThreeItems.collect().asList().await().indefinitely()).containsExactly(4, 5, 6, 7, 8, 9);
        assertThat(skipLastThreeItems.collect().asList().await().indefinitely()).containsExactly(1, 2, 3, 4 ,5, 6);
        assertThat(skipWhile.collect().asList().await().indefinitely()).containsExactly(4, 5, 6, 7, 8, 9);
        assertThat(skipForDuration.collect().asList().await().indefinitely()).isEmpty();

    }
}
