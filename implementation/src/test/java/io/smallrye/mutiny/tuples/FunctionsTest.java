package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testFunction6() {
        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4,
                item5, item6) -> item1 + item2 + item3 - item4 - item5 + item6;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6)).isEqualTo(3);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6))).isEqualTo(3);
    }

    @Test
    public void testFunction7() {
        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3,
                item4,
                item5, item6, item7) -> item1 + item2 + item3 - item4 - item5 + item6 + item7;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7)).isEqualTo(10);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7))).isEqualTo(10);
    }

    @Test
    public void testFunction8() {
        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2,
                item3, item4, item5, item6, item7, item8) -> item1 + item2 + item3 - item4 - item5 + item6 + item7 + item8;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7, 8)).isEqualTo(18);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8))).isEqualTo(18);
    }

    @Test
    public void testFunction9() {
        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (
                item1, item2, item3, item4, item5, item6, item7, item8,
                item9) -> item1 + item2 + item3 - item4 - item5 + item6 + item7 + item8 + item9;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7, 8, 9)).isEqualTo(27);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))).isEqualTo(27);
    }

}
