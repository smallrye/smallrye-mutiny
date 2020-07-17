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
package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MultiConvertToTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlux() {
        Flux<Integer> flux = Multi.createFrom().item(1).convert().with(MultiReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFluxFromNull() {
        Flux<Integer> flux = Multi.createFrom().item((Integer) null).convert().with(MultiReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFluxWithFailure() {
        Flux<Integer> flux = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        try {
            flux.blockFirst();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMono() {
        Mono<Integer> mono = Multi.createFrom().item(1).convert().with(MultiReactorConverters.toMono());
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMonoFromNull() {
        Mono<Integer> mono = Multi.createFrom().item((Integer) null).convert().with(MultiReactorConverters.toMono());
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMonoWithFailure() {
        Mono<Integer> mono = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiReactorConverters.toMono());
        assertThat(mono).isNotNull();
        try {
            mono.block();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }
}
