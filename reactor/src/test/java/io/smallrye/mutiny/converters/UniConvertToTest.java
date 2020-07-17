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

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UniConvertToTest {

    @Test
    public void testCreatingAFlux() {
        Flux<Integer> flux = Uni.createFrom().item(1).convert().with(UniReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isEqualTo(1);
    }

    @Test
    public void testCreatingAFluxFromNull() {
        Flux<Integer> flux = Uni.createFrom().item((Integer) null).convert().with(UniReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isNull();
    }

    @Test
    public void testCreatingAFluxWithFailure() {
        Flux<Integer> flux = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniReactorConverters.toFlux());
        assertThat(flux).isNotNull();
        try {
            flux.blockFirst();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingAMono() {
        Mono<Integer> mono = Uni.createFrom().item(1).convert().with(UniReactorConverters.toMono());
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo(1);
    }

    @Test
    public void testCreatingAMonoFromNull() {
        Mono<Integer> mono = Uni.createFrom().item((Integer) null).convert().with(UniReactorConverters.toMono());
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isNull();
    }

    @Test
    public void testCreatingAMonoWithFailure() {
        Mono<Integer> mono = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniReactorConverters.toMono());
        assertThat(mono).isNotNull();
        try {
            mono.block();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }
}
