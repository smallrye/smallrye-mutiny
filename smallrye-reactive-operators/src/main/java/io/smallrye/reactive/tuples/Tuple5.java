package io.smallrye.reactive.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple5<T1, T2, T3, T4, T5> extends Tuple4<T1, T2, T3, T4> implements Tuple {

    final T5 result5;

    Tuple5(T1 a, T2 b, T3 c, T4 d, T5 e) {
        super(a, b, c, d);
        this.result5 = e;
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 a, T2 b, T3 c, T4 d, T5 e) {
        return new Tuple5<>(a, b, c, d, e);
    }

    public T5 getResult5() {
        return result5;
    }

    @Override
    public <T> Tuple5<T, T2, T3, T4, T5> mapResult1(Function<T1, T> mapper) {
        return Tuple5.of(mapper.apply(result1), result2, result3, result4, result5);
    }

    @Override
    public <T> Tuple5<T1, T, T3, T4, T5> mapResult2(Function<T2, T> mapper) {
        return Tuple5.of(result1, mapper.apply(result2), result3, result4, result5);
    }

    @Override
    public <T> Tuple5<T1, T2, T, T4, T5> mapResult3(Function<T3, T> mapper) {
        return Tuple5.of(result1, result2, mapper.apply(result3), result4, result5);
    }

    @Override
    public <T> Tuple5<T1, T2, T3, T, T5> mapResult4(Function<T4, T> mapper) {
        return Tuple5.of(result1, result2, result3, mapper.apply(result4), result5);
    }

    public <T> Tuple5<T1, T2, T3, T4, T> mapResult5(Function<T5, T> mapper) {
        return Tuple5.of(result1, result2, result3, result4, mapper.apply(result5));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        if (index < size() - 1) {
            return super.nth(index);
        }

        return result5;
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(result1, result2, result3, result4, result5);
    }

    @Override
    public int size() {
        return 5;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "result1=" + result1 +
                ",result2=" + result2 +
                ",result3=" + result3 +
                ",result4=" + result4 +
                ",result5=" + result5 +
                '}';
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
        Tuple5<?, ?, ?, ?, ?> tuple5 = (Tuple5<?, ?, ?, ?, ?>) o;
        return Objects.equals(result5, tuple5.result5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), result5);
    }
}
