package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class Tuple6Test {

    private final Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> someTuple = new Tuple6<>(1, 2, 3, 4, 5, 6);

    @Test
    public void assertNullValues() {
        assertThat(Tuple6.of(null, 1, 2, 3, 4, 5)).containsExactly(null, 1, 2, 3, 4, 5);
        assertThat(Tuple6.of(1, null, 2, 3, 4, 5)).containsExactly(1, null, 2, 3, 4, 5);
        assertThat(Tuple6.of(1, 2, null, 3, 4, 5)).containsExactly(1, 2, null, 3, 4, 5);
        assertThat(Tuple6.of(1, 2, 3, null, 4, 5)).containsExactly(1, 2, 3, null, 4, 5);
        assertThat(Tuple6.of(1, 2, 3, null, 4, 5)).containsExactly(1, 2, 3, null, 4, 5);
        assertThat(Tuple6.of(1, 2, 3, 4, null, 5)).containsExactly(1, 2, 3, 4, null, 5);
        assertThat(Tuple6.of(null, null, null, null, null, null)).containsExactly(null, null, null, null, null, null);
    }

    @Test
    public void testMappingMethods() {
        assertThat(someTuple.mapItem1(i -> i + 1)).containsExactly(2, 2, 3, 4, 5, 6);
        assertThat(someTuple.mapItem2(i -> i + 1)).containsExactly(1, 3, 3, 4, 5, 6);
        assertThat(someTuple.mapItem3(i -> i + 1)).containsExactly(1, 2, 4, 4, 5, 6);
        assertThat(someTuple.mapItem4(i -> i + 1)).containsExactly(1, 2, 3, 5, 5, 6);
        assertThat(someTuple.mapItem5(i -> i + 1)).containsExactly(1, 2, 3, 4, 6, 6);
        assertThat(someTuple.mapItem6(i -> i + 1)).containsExactly(1, 2, 3, 4, 5, 7);
    }

    @Test
    public void testAccessingNegative() {
        assertThrows(IndexOutOfBoundsException.class, () -> someTuple.nth(-1));
    }

    @Test
    public void testAccessingOutOfIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> someTuple.nth(10));
    }

    @Test
    public void testNth() {
        assertThat(someTuple.nth(0)).isEqualTo(1);
        assertThat(someTuple.nth(1)).isEqualTo(2);
        assertThat(someTuple.nth(2)).isEqualTo(3);
        assertThat(someTuple.nth(3)).isEqualTo(4);
        assertThat(someTuple.nth(4)).isEqualTo(5);
        assertThat(someTuple.nth(5)).isEqualTo(6);
        assertThat(someTuple.getItem1()).isEqualTo(1);
        assertThat(someTuple.getItem2()).isEqualTo(2);
        assertThat(someTuple.getItem3()).isEqualTo(3);
        assertThat(someTuple.getItem4()).isEqualTo(4);
        assertThat(someTuple.getItem5()).isEqualTo(5);
        assertThat(someTuple.getItem6()).isEqualTo(6);
        assertThat(someTuple.size()).isEqualTo(6);
    }

    @Test
    public void testEquality() {
        assertThat(someTuple).isEqualTo(someTuple);
        assertThat(someTuple).isNotEqualTo(Tuple6.of(1, 2, 4, 5, 6, 10));
        assertThat(someTuple).isNotEqualTo("not a tuple");
        assertThat(someTuple).isEqualTo(Tuple6.of(1, 2, 3, 4, 5, 6));
    }

    @Test
    public void testHashCode() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> same = Tuple6.of(1, 2, 3, 4, 5, 6);
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> different = Tuple6.of(1, 2, 1, 4, 6, 10);

        assertThat(someTuple.hashCode())
                .isEqualTo(same.hashCode())
                .isNotEqualTo(different.hashCode());
    }

    @Test
    public void testFromList() {
        Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> tuple = Tuples.tuple6(Arrays.asList(1, 2, 3, 4, 5, 6));
        assertThat(tuple).containsExactly(1, 2, 3, 4, 5, 6);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple6(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple6(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple6(Arrays.asList(1, 2, 3, 4, 5, 6, 7)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.tuple6(null));
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= someTuple.size(); i++) {
            assertThat(someTuple.toString()).contains("item" + i + "=" + someTuple.nth(i - 1));
        }
    }
}
