package guides.integration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiReactorConverters;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class ReactorTest<T> {

    @Test
    public void testMultiCreation() {
        Flux<T> flux = getFlux();
        Mono<T> mono = getMono();

        // <reactor-multi-create>
        Multi<T> multiFromFlux = Multi.createFrom().publisher(flux);
        Multi<T> multiFromMono = Multi.createFrom().publisher(mono);
        // </reactor-multi-create>

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

        // <reactor-uni-create>
        Uni<T> uniFromFlux = Uni.createFrom().publisher(flux);
        Uni<T> uniFromMono = Uni.createFrom().publisher(mono);
        // </reactor-uni-create>

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

        // <reactor-create-multi>
        Flux<T> fluxFromMulti = Flux.from(multi);
        Mono<T> monoFromMulti = Mono.from(multi);
        // </reactor-create-multi>

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

        // <reactor-create-uni>
        Flux<T> fluxFromUni = uni.convert().with(UniReactorConverters.toFlux());
        Mono<T> monoFromUni = uni.convert().with(UniReactorConverters.toMono());
        // </reactor-create-uni>

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
        // <uni-export>
        Mono<String> mono = uni.convert().with(UniReactorConverters.toMono());
        Flux<String> flux = uni.convert().with(UniReactorConverters.toFlux());
        // </uni-export>

        assertThat(mono.block()).isEqualTo("hello");
        assertThat(flux.blockFirst()).isEqualTo("hello");
    }

    @Test
    public void multiExportToReactor() {
        Multi<String> multi = Multi.createFrom().items("hello", "bonjour");
        // <multi-export>
        Mono<String> mono = multi.convert().with(MultiReactorConverters.toMono());
        Flux<String> flux = multi.convert().with(MultiReactorConverters.toFlux());
        // </multi-export>

        assertThat(mono.block()).isEqualTo("hello");
        assertThat(flux.toIterable()).containsExactly("hello", "bonjour");
    }
}
