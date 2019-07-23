package io.smallrye.reactive.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A tuple containing two results.
 *
 * @param <L> The type of the first result
 * @param <R> The type of the second result
 */
public class Pair<L, R> implements Tuple {

    final L result1;
    final R result2;

    protected Pair(L left, R right) {
        this.result1 = left;
        this.result2 = right;
    }

    public static <L, R> Pair<L, R> of(L l, R r) {
        return new Pair<>(l, r);
    }

    /**
     * Gets the first result.
     *
     * @return The first result, can be {@code null}
     */
    public L getResult1() {
        return result1;
    }

    /**
     * Gets the second result.
     *
     * @return The second result, can be {@code null}
     */
    public R getResult2() {
        return result2;
    }

    /**
     * Applies a mapper function to the left (result1) part of this {@link Pair} to produce a new {@link Pair}.
     * The right part (result2) is not modified.
     *
     * @param mapper the mapping {@link Function} for the left result
     * @param <T>    the new type for the left result
     * @return the new {@link Pair}
     */
    public <T> Pair<T, R> mapResult1(Function<L, T> mapper) {
        return Pair.of(mapper.apply(result1), result2);
    }

    /**
     * Applies a mapper function to the right part (result2) of this {@link Pair} to produce a new {@link Pair}.
     * The left (result1) part is not modified.
     *
     * @param mapper the mapping {@link Function} for the right result
     * @param <T>    the new type for the right result
     * @return the new {@link Pair}
     */
    public <T> Pair<L, T> mapResult2(Function<R, T> mapper) {
        return Pair.of(result1, mapper.apply(result2));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);
        if (index == 0) {
            return result1;
        }
        return result2;
    }

    protected void assertIndexInBounds(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "Cannot retrieve result at position " + index + ", size is " + size());
        }
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(result1, result2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pair)) {
            return false;
        }

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!Objects.equals(result1, pair.result1)) {
            return false;
        }
        return Objects.equals(result2, pair.result2);
    }

    @Override
    public int hashCode() {
        int result = result1 != null ? result1.hashCode() : 0;
        result = 31 * result + (result2 != null ? result2.hashCode() : 0);
        return result;
    }

    public int size() {
        return 2;
    }

    @Override
    public String toString() {
        return "Pair{left=" + result1 + ", right=" + result2 + '}';
    }
}
