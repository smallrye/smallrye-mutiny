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

public class UniConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        Uni<Integer> uni = Uni.createFrom().converter(UniReactorConverters.fromMono(), Mono.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        Uni<Void> uni = Uni.createFrom().converter(UniReactorConverters.fromMono(), Mono.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniReactorConverters.fromMono(), Mono.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlux() {
        Uni<Integer> uni = Uni.createFrom().converter(UniReactorConverters.fromFlux(), Flux.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        Uni<Integer> uni = Uni.createFrom().converter(UniReactorConverters.fromFlux(), Flux.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        Uni<Void> uni = Uni.createFrom().converter(UniReactorConverters.fromFlux(), Flux.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniReactorConverters.fromFlux(), Flux.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

}
