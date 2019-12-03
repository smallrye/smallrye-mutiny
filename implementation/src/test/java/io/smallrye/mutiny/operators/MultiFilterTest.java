package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class MultiFilterTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatPredicateCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatFunctionCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .transform().byTestingItemsWith(null);
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .transform().byFilteringItemsWith(test)
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .transform()
                .byTestingItemsWith(
                        x -> Uni.createFrom().deferredCompletionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collectItems().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

}
