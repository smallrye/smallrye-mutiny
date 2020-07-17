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

import static io.smallrye.mutiny.unchecked.Unchecked.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.Supplier;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

public class UncheckedSupplierTest {

    @Test
    public void testUncheckedSupplier() {
        Supplier<Integer> supplier = supplier(() -> 1);
        Supplier<Integer> supplierFailingWithIo = supplier(() -> {
            throw new IOException("boom");
        });
        Supplier<Integer> supplierFailingWithArithmetic = supplier(() -> {
            throw new ArithmeticException("boom");
        });

        assertThat(supplier.get()).isEqualTo(1);
        assertThatThrownBy(supplierFailingWithIo::get)
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThatThrownBy(supplierFailingWithArithmetic::get)
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");
    }

    @Test
    public void testCreatingAUni() {
        assertThat(Uni.createFrom().item(supplier(() -> 1)).await().indefinitely()).isEqualTo(1);
        assertThatThrownBy(() -> Uni.createFrom().item(supplier(() -> {
            throw new IOException("boom");
        })).await().indefinitely()).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
    }

}
