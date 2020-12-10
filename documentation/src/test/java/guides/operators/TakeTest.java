package guides.operators;

import io.smallrye.mutiny.Multi;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class TakeTest {

    @Test
    public void testTake() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9);

        // tag::take-first[]
        Multi<Integer> firstThreeItems = multi.transform().byTakingFirstItems(3);
        // end::take-first[]

        // tag::take-last[]
        Multi<Integer> lastThreeItems = multi.transform().byTakingLastItems(3);
        // end::take-last[]

        // tag::take-while[]
        Multi<Integer> takeWhile = multi.transform().byTakingItemsWhile(i -> i < 4);
        // end::take-while[]

        // tag::take-for[]
        Multi<Integer> takeForDuration = multi.transform().byTakingItemsFor(Duration.ofSeconds(1));
        // end::take-for[]

        assertThat(firstThreeItems.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3);
        assertThat(takeWhile.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3);
        assertThat(lastThreeItems.collectItems().asList().await().indefinitely()).containsExactly(7, 8, 9);
        assertThat(takeForDuration.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);

    }

    @Test
    public void testSkip() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9);

        // tag::skip-first[]
        Multi<Integer> skipThreeItems = multi.transform().bySkippingFirstItems(3);
        // end::skip-first[]

        // tag::skip-last[]
        Multi<Integer>  skipLastThreeItems = multi.transform().bySkippingLastItems(3);
        // end::skip-last[]

        // tag::skip-while[]
        Multi<Integer> skipWhile = multi.transform().bySkippingItemsWhile(i -> i < 4);
        // end::skip-while[]

        // tag::skip-for[]
        Multi<Integer> skipForDuration = multi.transform().bySkippingItemsFor(Duration.ofSeconds(1));
        // end::skip-for[]

        assertThat(skipThreeItems.collectItems().asList().await().indefinitely()).containsExactly(4, 5, 6, 7, 8, 9);
        assertThat(skipLastThreeItems.collectItems().asList().await().indefinitely()).containsExactly(1, 2, 3, 4 ,5, 6);
        assertThat(skipWhile.collectItems().asList().await().indefinitely()).containsExactly(4, 5, 6, 7, 8, 9);
        assertThat(skipForDuration.collectItems().asList().await().indefinitely()).isEmpty();

    }
}
