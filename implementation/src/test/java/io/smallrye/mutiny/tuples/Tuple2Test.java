package io.smallrye.mutiny.tuples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

@SuppressWarnings("EqualsWithItself")
public class Tuple2Test {

    private final Tuple2<Integer, Integer> somePair = Tuple2.of(1, 3);

    @Test
    public void testThatNullIsAcceptedAsItem1() {
        Tuple2<Object, Integer> pair = Tuple2.of(null, 3);
        assertThat(pair).containsExactly(null, 3);
    }

    @Test
    public void testThatNullIsAcceptedAsItem2() {
        Tuple2<Object, Integer> pair = Tuple2.of(3, null);
        assertThat(pair).containsExactly(3, null);
    }

    @Test
    public void testWithStrings() {
        Tuple2<String, String> pair = Tuple2.of("a", "b");
        assertThat(pair.getItem1()).isEqualTo("a");
        assertThat(pair.getItem2()).isEqualTo("b");
    }

    @Test
    public void testWithStringAndInt() {
        Tuple2<String, Integer> pair = Tuple2.of("a", 1);
        assertThat(pair.getItem1()).isEqualTo("a");
        assertThat(pair.getItem2()).isEqualTo(1);
    }

    @Test
    public void testCreationWithNullValues() {
        Tuple2<String, String> pair = Tuple2.of(null, null);
        assertThat(pair.getItem1()).isNull();
        assertThat(pair.getItem2()).isNull();
    }

    @Test
    public void testEqualsWithStringAndInteger() {
        Tuple2<String, Integer> pair = Tuple2.of("a", 1);
        assertThat(pair).isEqualTo(pair);
        assertThat(pair).isNotEqualTo(null);
        assertThat(pair).isNotEqualTo("not a pair");
    }

    @Test
    public void testEqualsWithStringAndNull() {
        Tuple2<String, Integer> pair = Tuple2.of("a", null);
        assertThat(pair).isEqualTo(pair);
        assertThat(pair).isNotEqualTo(null);
        assertThat(pair).isNotEqualTo("not a pair");
        assertThat(pair).isEqualTo(Tuple2.of("a", null));
        assertThat(pair).isNotEqualTo(Tuple2.of(null, null));
    }

    @Test
    public void testHashCode() {
        Tuple2<String, Integer> pair = Tuple2.of("a", null);
        assertThat(pair.hashCode()).isEqualTo(pair.hashCode());
        assertThat(pair.hashCode()).isNotEqualTo("not a pair".hashCode());
    }

    @Test
    public void testMapItem1() {
        Tuple2<String, Integer> pair = Tuple2.of("Hello", 42);
        Tuple2<String, Integer> newPair = pair.mapItem1(String::toUpperCase);
        assertThat(newPair).containsExactly("HELLO", 42);

        newPair = pair.mapItem1(s -> null);
        assertThat(newPair).containsExactly(null, 42);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pair.mapItem1(s -> {
                    throw new IllegalArgumentException("boom");
                })).withMessage("boom");
    }

    @Test
    public void testMapItem2() {
        Tuple2<String, Integer> pair = Tuple2.of("Hello", 42);
        Tuple2<String, String> newPair = pair.mapItem2(i -> Integer.toString(i));
        assertThat(newPair).containsExactly("Hello", "42");

        newPair = pair.mapItem2(i -> null);
        assertThat(newPair).containsExactly("Hello", null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pair.mapItem2(s -> {
                    throw new IllegalArgumentException("boom");
                })).withMessage("boom");
    }

    @Test
    public void testAccessingNegative() {
        assertThrows(IndexOutOfBoundsException.class, () -> somePair.nth(-1));
    }

    @Test
    public void testAccessingOutOfIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> somePair.nth(3));
    }

    @Test
    public void testNth() {
        assertThat(somePair.nth(0)).isEqualTo(1);
        assertThat(somePair.nth(1)).isEqualTo(3);
        assertThat(somePair.getItem1()).isEqualTo(1);
        assertThat(somePair.getItem2()).isEqualTo(3);
        assertThat(somePair.size()).isEqualTo(2);
    }

    @Test
    public void testEquality() {
        assertThat(somePair).isEqualTo(somePair);
        assertThat(somePair.equals(somePair)).isTrue();
        assertThat(somePair).isNotEqualTo(Tuple2.of(1, 1));
        assertThat(somePair).isNotEqualTo("not a pair");
        assertThat(somePair).isEqualTo(Tuple2.of(1, 3));
    }

    @Test
    public void testFromList() {
        Tuple2<Integer, Integer> pair = Tuples.tuple2(Arrays.asList(1, 2));
        assertThat(pair).containsExactly(1, 2);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple2(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple2(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple2(Arrays.asList(1, 2, 3)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.tuple2(null));
    }

    @Test
    public void testToString() {
        assertThat(somePair.toString()).contains("item1=1", "item2=3");
    }

}
