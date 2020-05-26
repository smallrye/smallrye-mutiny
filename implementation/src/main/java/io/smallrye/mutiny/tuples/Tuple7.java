package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends Tuple6<T1, T2, T3, T4, T5, T6> implements Tuple { //NOSONAR

    final T7 item7;

    Tuple7(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f, T7 g) {
        super(a, b, c, d, e, f);
        this.item7 = g;
    }

    public static <T1, T2, T3, T4, T5, T6, T7> Tuple7<T1, T2, T3, T4, T5, T6, T7> of(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f, T7 g) {
        return new Tuple7<>(a, b, c, d, e, f, g);
    }

    public T7 getItem7() {
        return item7;
    }

    @Override
    public <T> Tuple7<T, T2, T3, T4, T5, T6, T7> mapItem1(Function<T1, T> mapper) {
        return Tuple7.of(mapper.apply(item1), item2, item3, item4, item5, item6, item7);
    }

    @Override
    public <T> Tuple7<T1, T, T3, T4, T5, T6, T7> mapItem2(Function<T2, T> mapper) {
        return Tuple7.of(item1, mapper.apply(item2), item3, item4, item5, item6, item7);
    }

    @Override
    public <T> Tuple7<T1, T2, T, T4, T5, T6, T7> mapItem3(Function<T3, T> mapper) {
        return Tuple7.of(item1, item2, mapper.apply(item3), item4, item5, item6, item7);
    }

    @Override
    public <T> Tuple7<T1, T2, T3, T, T5, T6, T7> mapItem4(Function<T4, T> mapper) {
        return Tuple7.of(item1, item2, item3, mapper.apply(item4), item5, item6, item7);
    }

    @Override
    public <T> Tuple7<T1, T2, T3, T4, T, T6, T7> mapItem5(Function<T5, T> mapper) {
        return Tuple7.of(item1, item2, item3, item4, mapper.apply(item5), item6, item7);
    }

    @Override
    public <T> Tuple7<T1, T2, T3, T4, T5, T, T7> mapItem6(Function<T6, T> mapper) {
        return Tuple7.of(item1, item2, item3, item4, item5, mapper.apply(item6), item7);
    }

    public <T> Tuple7<T1, T2, T3, T4, T5, T6, T> mapItem7(Function<T7, T> mapper) {
        return Tuple7.of(item1, item2, item3, item4, item5, item6, mapper.apply(item7));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        assertIndexInBounds(index);

        if (index == 6) {
            return item7;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3, item4, item5, item6, item7);
    }

    @Override
    public int size() {
        return 7;
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
        Tuple7<?, ?, ?, ?, ?, ?, ?> tuple = (Tuple7<?, ?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(item7, tuple.item7);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item7);
    }
}
