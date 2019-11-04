package io.smallrye.reactive.converters.uni;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.converters.UniConverter;
import reactor.core.publisher.Mono;

public class FromMono<T> implements UniConverter<Mono<T>, T> {

    public final static FromMono INSTANCE = new FromMono();

    private FromMono() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Mono<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
