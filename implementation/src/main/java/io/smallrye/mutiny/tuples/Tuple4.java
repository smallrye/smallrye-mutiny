package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple4<T1, T2, T3, T4> extends Tuple3<T1, T2, T3> implements Tuple {

    final T4 item4;

    Tuple4(T1 a, T2 b, T3 c, T4 d) {
        super(a, b, c);
        this.item4 = d;
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(T1 a, T2 b, T3 c, T4 d) {
        return new Tuple4<>(a, b, c, d);
    }

    public T4 getItem4() {
        return item4;
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        if (index == 3) {
            return item4;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public <T> Tuple4<T, T2, T3, T4> mapItem1(Function<T1, T> mapper) {
        return Tuple4.of(mapper.apply(item1), item2, item3, item4);
    }

    @Override
    public <T> Tuple4<T1, T, T3, T4> mapItem2(Function<T2, T> mapper) {
        return Tuple4.of(item1, mapper.apply(item2), item3, item4);
    }

    @Override
    public <T> Tuple4<T1, T2, T, T4> mapItem3(Function<T3, T> mapper) {
        return Tuple4.of(item1, item2, mapper.apply(item3), item4);
    }

    public <T> Tuple4<T1, T2, T3, T> mapItem4(Function<T4, T> mapper) {
        return Tuple4.of(item1, item2, item3, mapper.apply(item4));
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3, item4);
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
        return Objects.equals(item4, tuple4.item4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item4);
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "item1=" + item1 +
                ",item2=" + item2 +
                ",item3=" + item3 +
                ",item4=" + item4 +
                '}';
    }
}
