/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.operators.MultiItemCombine2;
import io.smallrye.mutiny.operators.MultiItemCombine3;
import io.smallrye.mutiny.operators.MultiItemCombine4;
import io.smallrye.mutiny.operators.MultiItemCombine5;
import io.smallrye.mutiny.operators.MultiItemCombine6;
import io.smallrye.mutiny.operators.MultiItemCombine7;
import io.smallrye.mutiny.operators.MultiItemCombine8;
import io.smallrye.mutiny.operators.MultiItemCombine9;
import io.smallrye.mutiny.operators.MultiItemCombineIterable;

public class MultiItemCombination {

    /**
     * Combines 2 streams.
     *
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
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
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
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
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
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
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
     * @param e the fifth stream, must not be {@code null}
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
     * Combines 6 streams.
     *
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
     * @param e the fifth stream, must not be {@code null}
     * @param f the sixth stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @param <T5> the type of item from the fifth stream
     * @param <T6> the type of item from the sixth stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4, T5, T6> MultiItemCombine6<T1, T2, T3, T4, T5, T6> streams(Publisher<? extends T1> a,
            Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d,
            Publisher<? extends T5> e, Publisher<? extends T6> f) {
        return new MultiItemCombine6<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d"),
                        nonNull(e, "e"), nonNull(f, "f")));
    }

    /**
     * Combines 7 streams.
     *
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
     * @param e the fifth stream, must not be {@code null}
     * @param f the sixth stream, must not be {@code null}
     * @param g the seventh stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @param <T5> the type of item from the fifth stream
     * @param <T6> the type of item from the sixth stream
     * @param <T7> the type of item from the seventh stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4, T5, T6, T7> MultiItemCombine7<T1, T2, T3, T4, T5, T6, T7> streams( // NOSONAR
            Publisher<? extends T1> a, Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d,
            Publisher<? extends T5> e, Publisher<? extends T6> f, Publisher<? extends T7> g) {
        return new MultiItemCombine7<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d"),
                        nonNull(e, "e"), nonNull(f, "f"), nonNull(g, "g")));
    }

    /**
     * Combines 8 streams.
     *
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
     * @param e the fifth stream, must not be {@code null}
     * @param f the sixth stream, must not be {@code null}
     * @param g the seventh stream, must not be {@code null}
     * @param h the eighth stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @param <T5> the type of item from the fifth stream
     * @param <T6> the type of item from the sixth stream
     * @param <T7> the type of item from the seventh stream
     * @param <T8> the type of item from the eighth stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8> MultiItemCombine8<T1, T2, T3, T4, T5, T6, T7, T8> streams( // NOSONAR
            Publisher<? extends T1> a, Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d,
            Publisher<? extends T5> e, Publisher<? extends T6> f, Publisher<? extends T7> g, Publisher<? extends T8> h) {
        return new MultiItemCombine8<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d"),
                        nonNull(e, "e"), nonNull(f, "f"), nonNull(g, "g"), nonNull(h, "h")));
    }

    /**
     * Combines 9 streams.
     *
     * @param a the first stream, must not be {@code null}
     * @param b the second stream, must not be {@code null}
     * @param c the third stream, must not be {@code null}
     * @param d the fourth stream, must not be {@code null}
     * @param e the fifth stream, must not be {@code null}
     * @param f the sixth stream, must not be {@code null}
     * @param g the seventh stream, must not be {@code null}
     * @param h the eighth stream, must not be {@code null}
     * @param i the ninth stream, must not be {@code null}
     * @param <T1> the type of item from the first stream
     * @param <T2> the type of item from the second stream
     * @param <T3> the type of item from the third stream
     * @param <T4> the type of item from the fourth stream
     * @param <T5> the type of item from the fifth stream
     * @param <T6> the type of item from the sixth stream
     * @param <T7> the type of item from the seventh stream
     * @param <T8> the type of item from the eighth stream
     * @param <T9> the type of item from the ninth stream
     * @return the object to configure the combination process
     */
    public <T1, T2, T3, T4, T5, T6, T7, T8, T9> MultiItemCombine9<T1, T2, T3, T4, T5, T6, T7, T8, T9> streams( // NOSONAR
            Publisher<? extends T1> a, Publisher<? extends T2> b, Publisher<? extends T3> c, Publisher<? extends T4> d,
            Publisher<? extends T5> e, Publisher<? extends T6> f, Publisher<? extends T7> g, Publisher<? extends T8> h,
            Publisher<? extends T9> i) {
        return new MultiItemCombine9<>(
                Arrays.asList(nonNull(a, "a"), nonNull(b, "b"), nonNull(c, "c"), nonNull(d, "d"),
                        nonNull(e, "e"), nonNull(f, "f"), nonNull(g, "g"), nonNull(h, "h"),
                        nonNull(i, "i")));
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
