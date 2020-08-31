package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.Test;

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
