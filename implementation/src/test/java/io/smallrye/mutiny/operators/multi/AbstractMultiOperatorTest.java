package io.smallrye.mutiny.operators.multi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;

class AbstractMultiOperatorTest {

    @Test
    public void test() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3);
        AbstractMultiOperator<Integer, Integer> operator = new AbstractMultiOperator<Integer, Integer>(multi) {
            // Do nothing
        };

        assertThat(operator.upstream()).isEqualTo(multi);

        assertThatThrownBy(() -> {
            new AbstractMultiOperator<Integer, Integer>(null) {
                // Do nothing
            };
        }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("upstream");
    }

}
