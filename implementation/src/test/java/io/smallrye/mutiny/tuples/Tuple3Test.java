package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

@SuppressWarnings("EqualsWithItself")
public class Tuple3Test {

    private final Tuple3<Integer, Integer, Integer> someTuple = new Tuple3<>(1, 2, 3);

    @Test
    public void assertNullValues() {
        assertThat(Tuple3.of(null, 1, 2)).containsExactly(null, 1, 2);
        assertThat(Tuple3.of(1, null, 2)).containsExactly(1, null, 2);
        assertThat(Tuple3.of(1, 2, null)).containsExactly(1, 2, null);
        assertThat(Tuple3.of(null, null, null)).containsExactly(null, null, null);
    }

    @Test
    public void testMappingMethods() {
        assertThat(someTuple.mapItem1(i -> i + 1)).containsExactly(2, 2, 3);
        assertThat(someTuple.mapItem2(i -> i + 1)).containsExactly(1, 3, 3);
        assertThat(someTuple.mapItem3(i -> i + 1)).containsExactly(1, 2, 4);
    }

    @Test
    public void testAccessingNegative() {
        assertThrows(IndexOutOfBoundsException.class, () -> someTuple.nth(-1));
    }

    @Test
    public void testAccessingOutOfIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> someTuple.nth(4));
    }

    @Test
    public void testNth() {
        assertThat(someTuple.nth(0)).isEqualTo(1);
        assertThat(someTuple.nth(1)).isEqualTo(2);
        assertThat(someTuple.nth(2)).isEqualTo(3);
        assertThat(someTuple.getItem1()).isEqualTo(1);
        assertThat(someTuple.getItem2()).isEqualTo(2);
        assertThat(someTuple.getItem3()).isEqualTo(3);
        assertThat(someTuple.size()).isEqualTo(3);
    }

    @Test
    public void testEquality() {
        assertThat(someTuple).isEqualTo(someTuple);
        assertThat(someTuple.equals(someTuple)).isTrue();
        assertThat(someTuple).isNotEqualTo(Tuple3.of(1, 2, 4));
        assertThat(someTuple).isNotEqualTo("not a tuple");
        assertThat(someTuple).isEqualTo(Tuple3.of(1, 2, 3));
    }

    @Test
    public void testHashCode() {
        Tuple3<Integer, Integer, Integer> same = new Tuple3<>(1, 2, 3);
        Tuple3<Integer, Integer, Integer> different = new Tuple3<>(1, 2, 1);

        assertThat(someTuple.hashCode())
                .isEqualTo(same.hashCode())
                .isNotEqualTo(different.hashCode());
    }

    @Test
    public void testFromList() {
        Tuple3<Integer, Integer, Integer> tuple = Tuples.tuple3(Arrays.asList(1, 2, 3));
        assertThat(tuple).containsExactly(1, 2, 3);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple3(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple3(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple3(Arrays.asList(1, 2, 3, 4)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.tuple3(null));
    }

    @Test
    public void testToString() {
        for (int i = 1; i <= someTuple.size(); i++) {
            assertThat(someTuple.toString()).contains("item" + i + "=" + someTuple.nth(i - 1));
        }
    }
}
