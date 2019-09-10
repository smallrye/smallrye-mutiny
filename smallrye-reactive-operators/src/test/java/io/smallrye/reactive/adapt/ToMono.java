package io.smallrye.reactive.adapt;

import io.smallrye.reactive.Uni;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {
    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(uni.adapt().toPublisher());
    }
}
