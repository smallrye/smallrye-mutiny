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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

public class Tuple7Test {

    private Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> someTuple = new Tuple7<>(1, 2, 3, 4, 5, 6, 7);

    @Test
    public void assertNullValues() {
        assertThat(Tuple7.of(null, 1, 2, 3, 4, 5, 6)).containsExactly(null, 1, 2, 3, 4, 5, 6);
        assertThat(Tuple7.of(1, null, 2, 3, 4, 5, 6)).containsExactly(1, null, 2, 3, 4, 5, 6);
        assertThat(Tuple7.of(1, 2, null, 3, 4, 5, 6)).containsExactly(1, 2, null, 3, 4, 5, 6);
        assertThat(Tuple7.of(1, 2, 3, null, 4, 5, 6)).containsExactly(1, 2, 3, null, 4, 5, 6);
        assertThat(Tuple7.of(1, 2, 3, null, 4, 5, 6)).containsExactly(1, 2, 3, null, 4, 5, 6);
        assertThat(Tuple7.of(1, 2, 3, 4, null, 5, 6)).containsExactly(1, 2, 3, 4, null, 5, 6);
        assertThat(Tuple7.of(1, 2, 3, 4, 5, null, 6)).containsExactly(1, 2, 3, 4, 5, null, 6);
        assertThat(Tuple7.of(1, 2, 3, 4, 5, 6, null)).containsExactly(1, 2, 3, 4, 5, 6, null);
        assertThat(Tuple7.of(null, null, null, null, null, null, null))
                .containsExactly(null, null, null, null, null, null, null);
    }

    @Test
    public void testMappingMethods() {
        assertThat(someTuple.mapItem1(i -> i + 1)).containsExactly(2, 2, 3, 4, 5, 6, 7);
        assertThat(someTuple.mapItem2(i -> i + 1)).containsExactly(1, 3, 3, 4, 5, 6, 7);
        assertThat(someTuple.mapItem3(i -> i + 1)).containsExactly(1, 2, 4, 4, 5, 6, 7);
        assertThat(someTuple.mapItem4(i -> i + 1)).containsExactly(1, 2, 3, 5, 5, 6, 7);
        assertThat(someTuple.mapItem5(i -> i + 1)).containsExactly(1, 2, 3, 4, 6, 6, 7);
        assertThat(someTuple.mapItem6(i -> i + 1)).containsExactly(1, 2, 3, 4, 5, 7, 7);
        assertThat(someTuple.mapItem7(i -> i + 1)).containsExactly(1, 2, 3, 4, 5, 6, 8);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAccessingNegative() {
        someTuple.nth(-1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAccessingOutOfIndex() {
        someTuple.nth(10);
    }

    @Test
    public void testNth() {
        assertThat(someTuple.nth(0)).isEqualTo(1);
        assertThat(someTuple.nth(1)).isEqualTo(2);
        assertThat(someTuple.nth(2)).isEqualTo(3);
        assertThat(someTuple.nth(3)).isEqualTo(4);
        assertThat(someTuple.nth(4)).isEqualTo(5);
        assertThat(someTuple.nth(5)).isEqualTo(6);
        assertThat(someTuple.nth(6)).isEqualTo(7);
        assertThat(someTuple.getItem1()).isEqualTo(1);
        assertThat(someTuple.getItem2()).isEqualTo(2);
        assertThat(someTuple.getItem3()).isEqualTo(3);
        assertThat(someTuple.getItem4()).isEqualTo(4);
        assertThat(someTuple.getItem5()).isEqualTo(5);
        assertThat(someTuple.getItem6()).isEqualTo(6);
        assertThat(someTuple.getItem7()).isEqualTo(7);
        assertThat(someTuple.size()).isEqualTo(7);
    }

    @Test
    public void testEquality() {
        assertThat(someTuple).isEqualTo(someTuple);
        assertThat(someTuple).isNotEqualTo(Tuple7.of(1, 2, 4, 5, 6, 7, 10));
        assertThat(someTuple).isNotEqualTo("not a tuple");
        assertThat(someTuple).isEqualTo(Tuple7.of(1, 2, 3, 4, 5, 6, 7));
    }

    @Test
    public void testHashCode() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> same = Tuple7.of(1, 2, 3, 4, 5, 6, 7);
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> different = Tuple7.of(1, 2, 1, 4, 6, 7, 10);

        assertThat(someTuple.hashCode())
                .isEqualTo(same.hashCode())
                .isNotEqualTo(different.hashCode());
    }

    @Test
    public void testFromList() {
        Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> tuple = Tuples
                .tuple7(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        assertThat(tuple).containsExactly(1, 2, 3, 4, 5, 6, 7);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple7(Collections.emptyList()));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple7(Collections.singletonList(1)));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> Tuples.tuple7(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Tuples.tuple7(null));
    }
}
