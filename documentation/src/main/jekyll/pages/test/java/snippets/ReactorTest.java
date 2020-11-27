package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorTest {

    @Test
    public void createUniFromReactor() {
        // tag::uni-create[]
        Mono<Void> empty = Mono.empty();
        Mono<String> mono = Mono.just("hello");
        Flux<String> flux = Flux.just("a", "b", "c");

        Uni<Void> uniFromEmptyMono = Uni.createFrom().converter(UniReactorConverters.fromMono(), empty);
        Uni<String> uniFromMono = Uni.createFrom().converter(UniReactorConverters.fromMono(), mono);
        Uni<String> uniFromFlux = Uni.createFrom().converter(UniReactorConverters.fromFlux(), flux);
        Uni<String> uniFromPublisher = Uni.createFrom().publisher(flux);
        // end::uni-create[]

        assertThat(uniFromEmptyMono.await().indefinitely()).isNull();
        assertThat(uniFromMono.await().indefinitely()).isEqualTo("hello");
        assertThat(uniFromFlux.await().indefinitely()).isEqualTo("a");
        assertThat(uniFromPublisher.await().indefinitely()).isEqualTo("a");
    }

    @Test
    public void createMultiFromReactor() {
        // tag::multi-create[]
        Mono<Void> empty = Mono.empty();
        Mono<String> mono = Mono.just("hello");
        Flux<String> flux = Flux.just("a", "b", "c");

        Multi<Void> multiFromEmptyMono = Multi.createFrom()
                .converter(MultiReactorConverters.fromMono(), empty);
        Multi<String> multiFromMono = Multi.createFrom().converter(MultiReactorConverters.fromMono(), mono);
        Multi<String> multiFromFlux = Multi.createFrom().converter(MultiReactorConverters.fromFlux(), flux);
        Multi<String> multiFromPublisher = Multi.createFrom().publisher(flux);
        // end::multi-create[]

        assertThat(multiFromEmptyMono.collectItems().first().await().indefinitely()).isNull();
        assertThat(multiFromMono.collectItems().first().await().indefinitely()).isEqualTo("hello");
        assertThat(multiFromFlux.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
        assertThat(multiFromPublisher.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
    }

    @Test
    public void uniExportToReactor() {
        Uni<String> uni = Uni.createFrom().item("hello");
        // tag::uni-export[]
        Mono<String> mono = uni.convert().with(UniReactorConverters.toMono());
        Flux<String> flux = uni.convert().with(UniReactorConverters.toFlux());
        // end::uni-export[]

        assertThat(mono.block()).isEqualTo("hello");
        assertThat(flux.blockFirst()).isEqualTo("hello");
    }

    @Test
    public void multiExportToReactor() {
        Multi<String> multi = Multi.createFrom().items("hello", "bonjour");
        // tag::multi-export[]
        Mono<String> mono = multi.convert().with(MultiReactorConverters.toMono());
        Flux<String> flux = multi.convert().with(MultiReactorConverters.toFlux());
        // end::multi-export[]

        assertThat(mono.block()).isEqualTo("hello");
        assertThat(flux.toIterable()).containsExactly("hello", "bonjour");
    }
}
