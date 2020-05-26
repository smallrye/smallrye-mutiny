package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple3<T1, T2, T3> extends Tuple2<T1, T2> implements Tuple {

    final T3 item3;

    Tuple3(T1 a, T2 b, T3 c) {
        super(a, b);
        this.item3 = c;
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(T1 a, T2 b, T3 c) {
        return new Tuple3<>(a, b, c);
    }

    public T3 getItem3() {
        return item3;
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        if (index == 2) {
            return item3;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public <T> Tuple3<T, T2, T3> mapItem1(Function<T1, T> mapper) {
        return Tuple3.of(mapper.apply(item1), item2, item3);
    }

    @Override
    public <T> Tuple3<T1, T, T3> mapItem2(Function<T2, T> mapper) {
        return Tuple3.of(item1, mapper.apply(item2), item3);
    }

    public <T> Tuple3<T1, T2, T> mapItem3(Function<T3, T> mapper) {
        return Tuple3.of(item1, item2, mapper.apply(item3));
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3);
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item3);
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
        return item3.equals(tuple3.item3);
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "item1=" + item1 +
                ",item2=" + item2 +
                ",item3=" + item3 +
                '}';
    }
}
