package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

public class Tuple4Test {

    private final Tuple4<Integer, Integer, Integer, Integer> someTuple = new Tuple4<>(1, 2, 3, 4);

    @Test
    public void assertNullValues() {
        assertThat(Tuple4.of(null, 1, 2, 3)).containsExactly(null, 1, 2, 3);
        assertThat(Tuple4.of(1, null, 2, 3)).containsExactly(1, null, 2, 3);
        assertThat(Tuple4.of(1, 2, null, 3)).containsExactly(1, 2, null, 3);
        assertThat(Tuple4.of(1, 2, 3, null)).containsExactly(1, 2, 3, null);
        assertThat(Tuple4.of(null, null, null, null)).containsExactly(null, null, null, null);
    }

    @Test
    public void testMappingMethods() {
        assertThat(someTuple.mapItem1(i -> i + 1)).containsExactly(2, 2, 3, 4);
        assertThat(someTuple.mapItem2(i -> i + 1)).containsExactly(1, 3, 3, 4);
        assertThat(someTuple.mapItem3(i -> i + 1)).containsExactly(1, 2, 4, 4);
        assertThat(someTuple.mapItem4(i -> i + 1)).containsExactly(1, 2, 3, 5);
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
        assertThat(someTuple.getItem1()).isEqualTo(1);
        assertThat(someTuple.getItem2()).isEqualTo(2);
        assertThat(someTuple.getItem3()).isEqualTo(3);
        assertThat(someTuple.getItem4()).isEqualTo(4);
        assertThat(someTuple.size()).isEqualTo(4);
    }

    @Test
    public void testEquality() {
        assertThat(someTuple).isEqualTo(someTuple);
        assertThat(someTuple).isNotEqualTo(Tuple4.of(1, 2, 4, 10));
        assertThat(someTuple).isNotEqualTo("not a tuple");
        assertThat(someTuple).isEqualTo(Tuple4.of(1, 2, 3, 4));
    }

    @Test
    public void testHashCode() {
        Tuple4<Integer, Integer, Integer, Integer> same = Tuple4.of(1, 2, 3, 4);
        Tuple4<Integer, Integer, Integer, Integer> different = Tuple4.of(1, 2, 1, 4);

        assertThat(someTuple.hashCode())
                .isEqualTo(same.hashCode())
                .isNotEqualTo(different.hashCode());
    }

    @Test
    public void testFromList() {
        Tuple4<Integer, Integer, Integer, Integer> tuple = Tuples.tuple4(Arrays.asList(1, 2, 3, 4));
        assertThat(tuple).containsExactly(1, 2, 3, 4);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple4(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple4(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple4(Arrays.asList(1, 2, 3, 4, 5)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.tuple4(null));
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= someTuple.size(); i++) {
            assertThat(someTuple.toString()).contains("item" + i + "=" + someTuple.nth(i - 1));
        }
    }
}
