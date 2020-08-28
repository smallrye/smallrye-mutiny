package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

/**
 * Reproducer for https://github.com/smallrye/smallrye-mutiny/issues/138.
 */
public class UniGenericHintTest {

    @Test
    public void testThatExactTypeCanBeSet() {
        Uni<Optional<Integer>> uni = Uni.createFrom().item(Collections.emptyList())
                .<Optional<Integer>> map(list -> {
                    if (list.isEmpty()) {
                        return Optional.empty();
                    } else {
                        return Optional.of(12345);
                    }
                })
                .onFailure().invoke(Throwable::printStackTrace);

        assertThat(uni.await().indefinitely()).isEmpty();
    }
}
