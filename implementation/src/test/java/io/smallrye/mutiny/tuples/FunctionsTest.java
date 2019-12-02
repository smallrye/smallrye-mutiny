package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.testng.annotations.Test;

public class FunctionsTest {

    @Test
    public void testFunction3() {
        Functions.Function3<Integer, Integer, Integer, Integer> fn = (item1, item2, item3) -> item1 + item2 + item3;
        assertThat(fn.apply(1, 2, 3)).isEqualTo(6);
        assertThat(fn.apply(Arrays.asList(1, 2, 3))).isEqualTo(6);
    }

    @Test
    public void testFunction4() {
        Functions.Function4<Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4) -> item1 + item2
                + item3 - item4;
        assertThat(fn.apply(1, 2, 3, 4)).isEqualTo(2);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4))).isEqualTo(2);
    }

    @Test
    public void testFunction5() {
        Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4,
                item5) -> item1 + item2 + item3 - item4 - item5;
        assertThat(fn.apply(1, 2, 3, 4, 5)).isEqualTo(-3);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(-3);
    }

}
