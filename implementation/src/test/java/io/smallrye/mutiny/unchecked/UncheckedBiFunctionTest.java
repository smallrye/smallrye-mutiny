package io.smallrye.mutiny.unchecked;

import static io.smallrye.mutiny.unchecked.Unchecked.function;
import static io.smallrye.mutiny.unchecked.Unchecked.unchecked;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

public class UncheckedBiFunctionTest {

    @Test
    public void testUncheckedBiFunction() throws Exception {
        BiFunction<Integer, Integer, Integer> sum = function(Integer::sum);
        BiFunction<Integer, Integer, Integer> failIo = function((i, j) -> {
            throw new IOException("boom");
        });
        BiFunction<Integer, Integer, Integer> failArithmetic = function((i, j) -> {
            throw new ArithmeticException("boom");
        });

        assertThat(sum.apply(1, 1)).isEqualTo(2);

        assertThatThrownBy(() -> failIo.apply(1, 2))
                .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        assertThatThrownBy(() -> failArithmetic.apply(1, 2))
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");

        assertThat(unchecked(sum).andThen(i -> i * 2).apply(1, 2)).isEqualTo(6);
        assertThat(unchecked(sum).andThen(this::validate).apply(1, 2)).isEqualTo(3);

        assertThatThrownBy(() -> unchecked(sum).andThen(this::validate).apply(0, 0)).isInstanceOf(IOException.class)
                .hasMessageContaining("boom");

        assertThatThrownBy(() -> unchecked(sum).andThen(this::validate).toBiFunction().apply(0, 0))
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
    }

    private int validate(int i) throws IOException {
        if (i != 0) {
            return i;
        } else {
            throw new IOException("boom");
        }
    }

}
