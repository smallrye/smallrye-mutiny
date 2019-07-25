package io.smallrye.reactive.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple3<T1, T2, T3> extends Pair<T1, T2> implements Tuple {

    final T3 result3;

    Tuple3(T1 a, T2 b, T3 c) {
        super(a, b);
        this.result3 = c;
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 a, T2 b, T3 c) {
        return new Tuple3<>(a, b, c);
    }

    public T3 getResult3() {
        return result3;
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        switch (index) {
            case 0:
                return result1;
            case 1:
                return result2;
            case 2:
                return result3;
            default:
                throw new IllegalArgumentException("invalid index " + index);
        }
    }

    @Override
    public <T> Tuple3<T, T2, T3> mapResult1(Function<T1, T> mapper) {
        return Tuple3.of(mapper.apply(result1), result2, result3);
    }

    @Override
    public <T> Tuple3<T1, T, T3> mapResult2(Function<T2, T> mapper) {
        return Tuple3.of(result1, mapper.apply(result2), result3);
    }

    public <T> Tuple3<T1, T2, T> mapResult3(Function<T3, T> mapper) {
        return Tuple3.of(result1, result2, mapper.apply(result3));
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(result1, result2, result3);
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), result3);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return result3.equals(tuple3.result3);
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "result1=" + result1 +
                ",result2=" + result2 +
                ",result3=" + result3 +
                '}';
    }
}
