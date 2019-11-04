package io.smallrye.reactive.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.converters.uni.ReactorConverters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UniConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        Uni<Integer> uni = Uni.createFrom().converter(ReactorConverters.fromMono(), Mono.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        Uni<Void> uni = Uni.createFrom().converter(ReactorConverters.fromMono(), Mono.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(ReactorConverters.fromMono(), Mono.error(new IOException("boom")));
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
        Uni<Integer> uni = Uni.createFrom().converter(ReactorConverters.fromFlux(), Flux.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        Uni<Integer> uni = Uni.createFrom().converter(ReactorConverters.fromFlux(), Flux.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        Uni<Void> uni = Uni.createFrom().converter(ReactorConverters.fromFlux(), Flux.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(ReactorConverters.fromFlux(), Flux.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

}
