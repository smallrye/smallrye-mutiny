package io.smallrye.reactive.tuples;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PairTest {

    // TODO Test tuples

    private Pair<Integer, Integer> somePair = Pair.of(1, 3);


    @Test
    public void testThatNullIsAcceptedAsItem1() {
        Pair<Object, Integer> pair = Pair.of(null, 3);
        assertThat(pair).containsExactly(null, 3);
    }

    @Test
    public void testThatNullIsAcceptedAsItem2() {
        Pair<Object, Integer> pair = Pair.of(3, null);
        assertThat(pair).containsExactly(3, null);
    }

    @Test
    public void testWithStrings() {
        Pair<String, String> pair = Pair.of("a", "b");
        assertThat(pair.getResult1()).isEqualTo("a");
        assertThat(pair.getResult2()).isEqualTo("b");
    }

    @Test
    public void testWithStringAndInt() {
        Pair<String, Integer> pair = Pair.of("a", 1);
        assertThat(pair.getResult1()).isEqualTo("a");
        assertThat(pair.getResult2()).isEqualTo(1);
    }

    @Test
    public void testCreationWithNullValues() {
        Pair<String, String> pair = Pair.of(null, null);
        assertThat(pair.getResult1()).isNull();
        assertThat(pair.getResult2()).isNull();
    }

    @Test
    public void testEqualsWithStringAndInteger() {
        Pair<String, Integer> pair = Pair.of("a", 1);
        assertThat(pair).isEqualTo(pair);
        assertThat(pair).isNotEqualTo(null);
        assertThat(pair).isNotEqualTo("not a pair");
    }

    @Test
    public void testEqualsWithStringAndNull() {
        Pair<String, Integer> pair = Pair.of("a", null);
        assertThat(pair).isEqualTo(pair);
        assertThat(pair).isNotEqualTo(null);
        assertThat(pair).isNotEqualTo("not a pair");
        assertThat(pair).isEqualTo(Pair.of("a", null));
        assertThat(pair).isNotEqualTo(Pair.of(null, null));
    }

    @Test
    public void testHashCode() {
        Pair<String, Integer> pair = Pair.of("a", null);
        assertThat(pair.hashCode()).isEqualTo(pair.hashCode());
        assertThat(pair.hashCode()).isNotEqualTo("not a pair".hashCode());
    }


    @Test
    public void testMapItem1() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        Pair<String, Integer> result = pair.mapResult1(String::toUpperCase);
        assertThat(result).containsExactly("HELLO", 42);

        result = pair.mapResult1(s -> null);
        assertThat(result).containsExactly(null, 42);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pair.mapResult1(s -> {
                    throw new IllegalArgumentException("boom");
                })).withMessage("boom");
    }

    @Test
    public void testMapItem2() {
        Pair<String, Integer> pair = Pair.of("Hello", 42);
        Pair<String, String> result = pair.mapResult2(i -> Integer.toString(i));
        assertThat(result).containsExactly("Hello", "42");

        result = pair.mapResult2(i -> null);
        assertThat(result).containsExactly("Hello", null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pair.mapResult2(s -> {
                    throw new IllegalArgumentException("boom");
                })).withMessage("boom");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAccessingNegative() {
        somePair.nth(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAccessingOutOfIndex() {
        somePair.nth(3);
    }

    @Test
    public void testNth() {
        assertThat(somePair.nth(0)).isEqualTo(1);
        assertThat(somePair.nth(1)).isEqualTo(3);
        assertThat(somePair.getResult1()).isEqualTo(1);
        assertThat(somePair.getResult2()).isEqualTo(3);
        assertThat(somePair.size()).isEqualTo(2);
    }


    @Test
    public void testEquality() {
        assertThat(somePair).isEqualTo(somePair);
        assertThat(somePair).isNotEqualTo(Pair.of(1, 1));
        assertThat(somePair).isNotEqualTo("not a pair");
        assertThat(somePair).isEqualTo(Pair.of(1, 3));
    }

    @Test
    public void testFromList() {
        Pair<Integer, Integer> pair = Tuples.pair(Arrays.asList(1, 2));
        assertThat(pair).containsExactly(1, 2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.pair(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.pair(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.pair(Arrays.asList(1, 2, 3)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.pair(null));
    }

}