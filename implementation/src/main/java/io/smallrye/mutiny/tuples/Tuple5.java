package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple5<T1, T2, T3, T4, T5> extends Tuple4<T1, T2, T3, T4> implements Tuple {

    final T5 item5;

    Tuple5(T1 a, T2 b, T3 c, T4 d, T5 e) {
        super(a, b, c, d);
        this.item5 = e;
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(T1 a, T2 b, T3 c, T4 d, T5 e) {
        return new Tuple5<>(a, b, c, d, e);
    }

    public T5 getItem5() {
        return item5;
    }

    @Override
    public <T> Tuple5<T, T2, T3, T4, T5> mapItem1(Function<T1, T> mapper) {
        return Tuple5.of(mapper.apply(item1), item2, item3, item4, item5);
    }

    @Override
    public <T> Tuple5<T1, T, T3, T4, T5> mapItem2(Function<T2, T> mapper) {
        return Tuple5.of(item1, mapper.apply(item2), item3, item4, item5);
    }

    @Override
    public <T> Tuple5<T1, T2, T, T4, T5> mapItem3(Function<T3, T> mapper) {
        return Tuple5.of(item1, item2, mapper.apply(item3), item4, item5);
    }

    @Override
    public <T> Tuple5<T1, T2, T3, T, T5> mapItem4(Function<T4, T> mapper) {
        return Tuple5.of(item1, item2, item3, mapper.apply(item4), item5);
    }

    public <T> Tuple5<T1, T2, T3, T4, T> mapItem5(Function<T5, T> mapper) {
        return Tuple5.of(item1, item2, item3, item4, mapper.apply(item5));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        if (index == 4) {
            return item5;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3, item4, item5);
    }

    @Override
    public int size() {
        return 5;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "item1=" + item1 +
                ",item2=" + item2 +
                ",item3=" + item3 +
                ",item4=" + item4 +
                ",item5=" + item5 +
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
        return Objects.equals(item5, tuple5.item5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item5);
    }
}
