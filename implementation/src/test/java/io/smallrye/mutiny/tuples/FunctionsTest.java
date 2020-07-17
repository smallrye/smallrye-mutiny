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

import java.util.Arrays;

import org.testng.annotations.Test;

public class FunctionsTest {

    @Test
    public void testFunction3() {
        Functions.Function3<Integer, Integer, Integer, Integer> fn = (item1, item2, item3) -> item1 + item2 + item3;
        assertThat(fn.apply(1, 2, 3)).isEqualTo(6);
        assertThat(fn.apply(Arrays.asList(1, 2, 3))).isEqualTo(6);
    }

    @Test
    public void testFunction4() {
        Functions.Function4<Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4) -> item1 + item2
                + item3 - item4;
        assertThat(fn.apply(1, 2, 3, 4)).isEqualTo(2);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4))).isEqualTo(2);
    }

    @Test
    public void testFunction5() {
        Functions.Function5<Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4,
                item5) -> item1 + item2 + item3 - item4 - item5;
        assertThat(fn.apply(1, 2, 3, 4, 5)).isEqualTo(-3);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(-3);
    }

    @Test
    public void testFunction6() {
        Functions.Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3, item4,
                item5, item6) -> item1 + item2 + item3 - item4 - item5 + item6;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6)).isEqualTo(3);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6))).isEqualTo(3);
    }

    @Test
    public void testFunction7() {
        Functions.Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2, item3,
                item4,
                item5, item6, item7) -> item1 + item2 + item3 - item4 - item5 + item6 + item7;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7)).isEqualTo(10);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7))).isEqualTo(10);
    }

    @Test
    public void testFunction8() {
        Functions.Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (item1, item2,
                item3, item4, item5, item6, item7, item8) -> item1 + item2 + item3 - item4 - item5 + item6 + item7 + item8;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7, 8)).isEqualTo(18);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8))).isEqualTo(18);
    }

    @Test
    public void testFunction9() {
        Functions.Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> fn = (
                item1, item2, item3, item4, item5, item6, item7, item8,
                item9) -> item1 + item2 + item3 - item4 - item5 + item6 + item7 + item8 + item9;
        assertThat(fn.apply(1, 2, 3, 4, 5, 6, 7, 8, 9)).isEqualTo(27);
        assertThat(fn.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9))).isEqualTo(27);
    }

}
