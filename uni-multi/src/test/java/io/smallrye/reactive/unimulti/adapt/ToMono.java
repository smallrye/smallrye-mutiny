package io.smallrye.reactive.unimulti.adapt;

import java.util.function.Function;

import io.smallrye.reactive.unimulti.Uni;
import reactor.core.publisher.Mono;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {
    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(uni.adapt().toPublisher());
    }
}
