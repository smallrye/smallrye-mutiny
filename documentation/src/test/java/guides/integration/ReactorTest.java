package guides.integration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class ReactorTest<T> {

    @Test
    public void testMultiCreation() {
        Flux<T> flux = getFlux();
        Mono<T> mono = getMono();

        // tag::reactor-multi-create[]
        Flow.Publisher<T> fluxAsPublisher = AdaptersToFlow.publisher(flux);
        Multi<T> multiFromFlux = Multi.createFrom().publisher(fluxAsPublisher);

        Flow.Publisher<T> monoAsPublisher = AdaptersToFlow.publisher(mono);
        Multi<T> multiFromMono = Multi.createFrom().publisher(monoAsPublisher);
        // end::reactor-multi-create[]

        List<String> list = multiFromFlux
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a", "b", "c");

        list = multiFromMono
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a");

    }

    @Test
    public void testUniCreation() {
        Flux<T> flux = getFlux();
        Mono<T> mono = getMono();

        // tag::reactor-uni-create[]
        Flow.Publisher<T> fluxAsPublisher = AdaptersToFlow.publisher(flux);
        Uni<T> uniFromFlux = Uni.createFrom().publisher(fluxAsPublisher);

        Flow.Publisher<T> monoAsPublisher = AdaptersToFlow.publisher(mono);
        Uni<T> uniFromMono = Uni.createFrom().publisher(monoAsPublisher);
        // end::reactor-uni-create[]

        String s = uniFromFlux
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");

        s = uniFromMono
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");
    }

    @Test
    public void testCreationFromMulti() {
        Multi<T> multi = getMulti();

        // tag::reactor-create-multi[]
        Publisher<T> multiAsLegacyPublisher = AdaptersToReactiveStreams.publisher(multi);

        Flux<T> fluxFromMulti = Flux.from(multiAsLegacyPublisher);
        Mono<T> monoFromMulti = Mono.from(multiAsLegacyPublisher);
        // end::reactor-create-multi[]

        List<String> list = fluxFromMulti
                .map(Object::toString)
                .collectList()
                .block();
        assertThat(list).containsExactly("a", "b", "c");

        String s = monoFromMulti
                .map(Object::toString)
                .block();
        assertThat(s).isEqualTo("a");
    }

    @Test
    public void testCreationFromUni() {
        Uni<T> uni = getUni();

        // tag::reactor-create-uni[]
        Flux<T> fluxFromUni = uni.convert().with(UniReactorConverters.toFlux());
        Mono<T> monoFromUni = uni.convert().with(UniReactorConverters.toMono());
        // end::reactor-create-uni[]

        List<String> list = fluxFromUni
                .map(Object::toString)
                .collectList()
                .block();
        assertThat(list).containsExactly("a");

        String s = monoFromUni
                .map(Object::toString)
                .block();
        assertThat(s).isEqualTo("a");
    }

    private Flux<T> getFlux() {
        return Flux.just("a", "b", "c")
                .map(s -> (T) s);
    }

    private Mono<T> getMono() {
        return Mono.just("a")
                .map(s -> (T) s);
    }

    private Multi<T> getMulti() {
        return Multi.createFrom().items("a", "b", "c")
                .map(s -> (T) s);
    }

    private Uni<T> getUni() {
        return Uni.createFrom().item("a")
                .map(s -> (T) s);
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
