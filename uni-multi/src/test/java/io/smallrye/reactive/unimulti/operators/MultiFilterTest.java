package io.smallrye.reactive.unimulti.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.junit.Test;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.Uni;

public class MultiFilterTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThatPredicateCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .onItem().filterWith(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatFunctionCannotBeNull() {
        Multi.createFrom().range(1, 4)
                .onItem().testWith(null);
    }

    @Test
    public void testFilteringWithPredicate() {
        Predicate<Integer> test = x -> x % 2 != 0;
        assertThat(Multi.createFrom().range(1, 4)
                .onItem().filterWith(test)
                .collect().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

    @Test
    public void testFilteringWithUni() {
        assertThat(Multi.createFrom().range(1, 4)
                .onItem()
                .testWith(x -> Uni.createFrom()
                        .deferredCompletionStage(() -> CompletableFuture.supplyAsync(() -> x % 2 != 0)))
                .collect().asList()
                .await().indefinitely()).containsExactly(1, 3);
    }

}
