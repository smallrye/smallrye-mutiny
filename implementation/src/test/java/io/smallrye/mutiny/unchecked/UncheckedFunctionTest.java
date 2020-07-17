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
package io.smallrye.mutiny.unchecked;

import static io.smallrye.mutiny.unchecked.Unchecked.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UncheckedFunctionTest {

    @Test
    public void testWithMap() {
        Integer res = Uni.createFrom().item(1)
                .map(function(i -> {
                    if (i == 0) {
                        throw new IOException("boom");
                    }
                    return i;
                })).await().indefinitely();

        assertThat(res).isEqualTo(1);

        assertThatThrownBy(() -> Uni.createFrom().item(0)
                .map(function(i -> {
                    if (i == 0) {
                        throw new IOException("boom");
                    }
                    return i;
                })).await().indefinitely()).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class)
                        .hasMessageContaining("boom");

        assertThatThrownBy(() -> Uni.createFrom().item(0)
                .map(function(i -> {
                    if (i == 0) {
                        throw new ArithmeticException("boom");
                    }
                    return i;
                })).await().indefinitely()).isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");
    }

    interface Reader {
        int read(int i) throws IOException;
    }

    interface UniReader {
        Uni<Integer> read(int i) throws IOException;
    }

    interface UniSupplier {
        Uni<Integer> get() throws IOException;
    }

    @Test
    public void testWithInterfaceMap() {
        Reader reader = i -> i;
        int res = Uni.createFrom().item(1)
                .map(function(reader::read))
                .await().indefinitely();
        assertThat(res).isEqualTo(1);
    }

    @Test
    public void testWithFlatMap() {
        UniReader reader = i -> Uni.createFrom().item(i);
        int res = Uni.createFrom().item(1)
                .flatMap(function(reader::read))
                .await().indefinitely();
        assertThat(res).isEqualTo(1);
    }

    @Test
    public void testWithChain() {
        UniReader reader = i -> Uni.createFrom().item(i);
        int res = Uni.createFrom().item(1)
                .chain(function(reader::read))
                .await().indefinitely();
        assertThat(res).isEqualTo(1);
    }

    @Test
    public void testWithThen() {
        UniSupplier reader = () -> Uni.createFrom().item(23);
        int res = Uni.createFrom().item(1)
                .then(supplier(reader::get))
                .await().indefinitely();
        assertThat(res).isEqualTo(23);
    }

    private int validate(int i) throws IOException {
        if (i != 0) {
            return i;
        } else {
            throw new IOException("boom");
        }
    }

    @Test
    public void testChaining() {
        UncheckedFunction<Integer, String> function = unchecked(i -> (String) i)
                .andThen(Integer::valueOf)
                .andThen(this::validate)
                .andThen(i -> Integer.toString(i))
                .compose(x -> x.toString() + x.toString());

        String res = function.toFunction().apply(1);
        assertThat(res).isEqualTo("11");

        assertThatThrownBy(() -> function.toFunction().apply(0))
                .hasCauseInstanceOf(IOException.class);
    }

}
