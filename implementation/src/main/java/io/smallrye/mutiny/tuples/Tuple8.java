package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> extends Tuple7<T1, T2, T3, T4, T5, T6, T7> implements Tuple { //NOSONAR

    final T8 item8;

    Tuple8(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f, T7 g, T8 h) { //NOSONAR
        super(a, b, c, d, e, f, g);
        this.item8 = h;
    }

    public static <T1, T2, T3, T4, T5, T6, T7, T8> Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> of(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f, //NOSONAR
            T7 g, T8 h) {
        return new Tuple8<>(a, b, c, d, e, f, g, h);
    }

    public T8 getItem8() {
        return item8;
    }

    @Override
    public <T> Tuple8<T, T2, T3, T4, T5, T6, T7, T8> mapItem1(Function<T1, T> mapper) {
        return Tuple8.of(mapper.apply(item1), item2, item3, item4, item5, item6, item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T, T3, T4, T5, T6, T7, T8> mapItem2(Function<T2, T> mapper) {
        return Tuple8.of(item1, mapper.apply(item2), item3, item4, item5, item6, item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T2, T, T4, T5, T6, T7, T8> mapItem3(Function<T3, T> mapper) {
        return Tuple8.of(item1, item2, mapper.apply(item3), item4, item5, item6, item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T2, T3, T, T5, T6, T7, T8> mapItem4(Function<T4, T> mapper) {
        return Tuple8.of(item1, item2, item3, mapper.apply(item4), item5, item6, item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T2, T3, T4, T, T6, T7, T8> mapItem5(Function<T5, T> mapper) {
        return Tuple8.of(item1, item2, item3, item4, mapper.apply(item5), item6, item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T2, T3, T4, T5, T, T7, T8> mapItem6(Function<T6, T> mapper) {
        return Tuple8.of(item1, item2, item3, item4, item5, mapper.apply(item6), item7, item8);
    }

    @Override
    public <T> Tuple8<T1, T2, T3, T4, T5, T6, T, T8> mapItem7(Function<T7, T> mapper) {
        return Tuple8.of(item1, item2, item3, item4, item5, item6, mapper.apply(item7), item8);
    }

    public <T> Tuple8<T1, T2, T3, T4, T5, T6, T7, T> mapItem8(Function<T8, T> mapper) {
        return Tuple8.of(item1, item2, item3, item4, item5, item6, item7, mapper.apply(item8));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        assertIndexInBounds(index);

        if (index == 7) {
            return item8;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3, item4, item5, item6, item7, item8);
    }

    @Override
    public int size() {
        return 8;
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "item1=" + item1 +
                ",item2=" + item2 +
                ",item3=" + item3 +
                ",item4=" + item4 +
                ",item5=" + item5 +
                ",item6=" + item6 +
                ",item7=" + item7 +
                ",item8=" + item8 +
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
        Tuple8<?, ?, ?, ?, ?, ?, ?, ?> tuple = (Tuple8<?, ?, ?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(item8, tuple.item8);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item8);
    }
}
