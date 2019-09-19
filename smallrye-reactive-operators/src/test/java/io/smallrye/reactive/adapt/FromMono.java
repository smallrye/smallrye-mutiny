package io.smallrye.reactive.adapt;

import io.smallrye.reactive.Uni;
import reactor.core.publisher.Mono;

public class FromMono<T> implements UniConverter<Mono<T>, T> {
    @Override
    public Uni<T> from(Mono<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
