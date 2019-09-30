package io.smallrye.reactive.unimulti.adapt;

import java.util.function.Function;

import io.smallrye.reactive.unimulti.Uni;
import reactor.core.publisher.Flux;

public class ToFlux<T> implements Function<Uni<T>, Flux<T>> {
    @Override
    public Flux<T> apply(Uni<T> uni) {
        return Flux.from(uni.adapt().toPublisher());
    }
}
