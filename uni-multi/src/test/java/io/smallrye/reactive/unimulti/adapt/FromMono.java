package io.smallrye.reactive.unimulti.adapt;

import io.smallrye.reactive.unimulti.Uni;
import reactor.core.publisher.Mono;

public class FromMono<T> implements UniConverter<Mono<T>, T> {
    @Override
    public Uni<T> from(Mono<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
