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
package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;

/**
 * Reproducer for https://github.com/smallrye/smallrye-mutiny/issues/138.
 */
public class UniGenericHintTest {

    @Test
    public void testThatExactTypeCanBeSet() {
        Uni<Optional<Integer>> uni = Uni.createFrom().item(Collections.emptyList())
                .<Optional<Integer>> map(list -> {
                    if (list.isEmpty()) {
                        return Optional.empty();
                    } else {
                        return Optional.of(12345);
                    }
                })
                .onFailure().invoke(Throwable::printStackTrace);

        assertThat(uni.await().indefinitely()).isEmpty();
    }
}
