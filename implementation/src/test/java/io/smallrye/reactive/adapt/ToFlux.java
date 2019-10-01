package io.smallrye.reactive.adapt;

import java.util.function.Function;

import io.smallrye.reactive.Uni;
import reactor.core.publisher.Flux;

public class ToFlux<T> implements Function<Uni<T>, Flux<T>> {
    @Override
    public Flux<T> apply(Uni<T> uni) {
        return Flux.from(uni.adapt().toPublisher());
    }
}
