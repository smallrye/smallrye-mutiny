package io.smallrye.reactive.converters.multi;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.MultiConverter;
import reactor.core.publisher.Mono;

public class FromMono<T> implements MultiConverter<Mono<T>, T> {

    public final static FromMono INSTANCE = new FromMono();

    private FromMono() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Mono<T> instance) {
        return Multi.createFrom().publisher(instance);
    }
}
