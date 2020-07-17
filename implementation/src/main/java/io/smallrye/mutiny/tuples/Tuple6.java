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
package io.smallrye.mutiny.tuples;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Tuple6<T1, T2, T3, T4, T5, T6> extends Tuple5<T1, T2, T3, T4, T5> implements Tuple {

    final T6 item6;

    Tuple6(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f) {
        super(a, b, c, d, e);
        this.item6 = f;
    }

    public static <T1, T2, T3, T4, T5, T6> Tuple6<T1, T2, T3, T4, T5, T6> of(T1 a, T2 b, T3 c, T4 d, T5 e, T6 f) {
        return new Tuple6<>(a, b, c, d, e, f);
    }

    public T6 getItem6() {
        return item6;
    }

    @Override
    public <T> Tuple6<T, T2, T3, T4, T5, T6> mapItem1(Function<T1, T> mapper) {
        return Tuple6.of(mapper.apply(item1), item2, item3, item4, item5, item6);
    }

    @Override
    public <T> Tuple6<T1, T, T3, T4, T5, T6> mapItem2(Function<T2, T> mapper) {
        return Tuple6.of(item1, mapper.apply(item2), item3, item4, item5, item6);
    }

    @Override
    public <T> Tuple6<T1, T2, T, T4, T5, T6> mapItem3(Function<T3, T> mapper) {
        return Tuple6.of(item1, item2, mapper.apply(item3), item4, item5, item6);
    }

    @Override
    public <T> Tuple6<T1, T2, T3, T, T5, T6> mapItem4(Function<T4, T> mapper) {
        return Tuple6.of(item1, item2, item3, mapper.apply(item4), item5, item6);
    }

    @Override
    public <T> Tuple6<T1, T2, T3, T4, T, T6> mapItem5(Function<T5, T> mapper) {
        return Tuple6.of(item1, item2, item3, item4, mapper.apply(item5), item6);
    }

    public <T> Tuple6<T1, T2, T3, T4, T5, T> mapItem6(Function<T6, T> mapper) {
        return Tuple6.of(item1, item2, item3, item4, item5, mapper.apply(item6));
    }

    @Override
    public Object nth(int index) {
        assertIndexInBounds(index);

        if (index == 5) {
            return item6;
        } else {
            return super.nth(index);
        }
    }

    @Override
    public List<Object> asList() {
        return Arrays.asList(item1, item2, item3, item4, item5, item6);
    }

    @Override
    public int size() {
        return 6;
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
        Tuple6<?, ?, ?, ?, ?, ?> tuple6 = (Tuple6<?, ?, ?, ?, ?, ?>) o;
        return Objects.equals(item6, tuple6.item6);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), item6);
    }
}
