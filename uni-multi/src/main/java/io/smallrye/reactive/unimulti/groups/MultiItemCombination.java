package io.smallrye.reactive.unimulti.groups;

import io.smallrye.reactive.unimulti.operators.*;
import org.reactivestreams.Publisher;

import java.util.Arrays;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

public class MultiItemCombination {

    /**
     * Combines 2 streams.
     *
     * @param a    the first stream, must not be {@code null}
     * @param b    the second stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @return the object to configure the combination process
     */
    public <T1, T2> MultiItemCombine2<T1, T2> streams(Publisher<? extends T1> a, Publisher<? extends T2> b) {
        return new MultiItemCombine2<>(Arrays.asList(nonNull(a, "a"), nonNull(b, "b")));
    }

    /**
     * Combines 3 streams.
     *
     * @param a    the first stream, must not be {@code null}
     * @param b    the second stream, must not be {@code null}
     * @param c    the third stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3> MultiItemCombine3<T1, T2, T3> streams(Publisher<? extends T1> a, Publisher<? extends T2> b,
            Publisher<? extends T3> c) {
        return new MultiItemCombine3<>(Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c")));
    }

    /**
     * Combines 4 streams.
     *
     * @param a    the first stream, must not be {@code null}
     * @param b    the second stream, must not be {@code null}
     * @param c    the third stream, must not be {@code null}
     * @param d    the fourth stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4> MultiItemCombine4<T1, T2, T3, T4> streams(Publisher<? extends T1> a,
            Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d) {
        return new MultiItemCombine4<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d")));
    }

    /**
     * Combines 5 streams.
     *
     * @param a    the first stream, must not be {@code null}
     * @param b    the second stream, must not be {@code null}
     * @param c    the third stream, must not be {@code null}
     * @param d    the fourth stream, must not be {@code null}
     * @param e    the fifth stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @param <T5> the type of item from the fifth stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4, T5> MultiItemCombine5<T1, T2, T3, T4, T5> streams(Publisher<? extends T1> a,
            Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d,
            Publisher<? extends T5> e) {
        return new MultiItemCombine5<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d"), nonNull(e, "e")));
    }

    /**
     * Combines multiple streams.
     *
     * @param iterable the iterable containing the streams to combine. Must not be {@code null}
     * @return the object to configure the combination process
     */
    public MultiItemCombineIterable streams(Iterable<? extends Publisher<?>> iterable) {
        return new MultiItemCombineIterable(nonNull(iterable, "iterable"));
    }

}
