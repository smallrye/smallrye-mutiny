package io.smallrye.reactive.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple4<T1, T2, T3, T4> extends Tuple3<T1, T2, T3> implements Tuple {

    final T4 result4;

    Tuple4(T1 a, T2 b, T3 c, T4 d) {
        super(a, b, c);
        this.result4 = d;
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 a, T2 b, T3 c, T4 d) {
        return new Tuple4<>(a, b, c, d);
    }

    public T4 getResult4() {
        return result4;
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
            case 3:
                return result4;
            default:
                throw new IllegalArgumentException("invalid index " + index);
        }
    }

    public <T> Tuple4<T, T2, T3, T4> mapResult1(Function<T1, T> mapper) {
        return Tuple4.of(mapper.apply(result1), result2, result3, result4);
    }

    public <T> Tuple4<T1, T, T3, T4> mapResult2(Function<T2, T> mapper) {
        return Tuple4.of(result1, mapper.apply(result2), result3, result4);
    }

    public <T> Tuple4<T1, T2, T, T4> mapResult3(Function<T3, T> mapper) {
        return Tuple4.of(result1, result2, mapper.apply(result3), result4);
    }

    public <T> Tuple4<T1, T2, T3, T> mapResult4(Function<T4, T> mapper) {
        return Tuple4.of(result1, result2, result3, mapper.apply(result4));
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(result1, result2, result3, result4);
    }

    @Override
    public int size() {
        return 4;
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
        Tuple4<?, ?, ?, ?> tuple4 = (Tuple4<?, ?, ?, ?>) o;
        return Objects.equals(result4, tuple4.result4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), result4);
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "result1=" + result1 +
                ",result2=" + result2 +
                ",result3=" + result3 +
                ",result4=" + result4 +
                '}';
    }
}
