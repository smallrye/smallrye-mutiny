package io.smallrye.reactive.unimulti.adapt;

import io.smallrye.reactive.unimulti.Uni;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {
    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(uni.adapt().toPublisher());
    }
}
