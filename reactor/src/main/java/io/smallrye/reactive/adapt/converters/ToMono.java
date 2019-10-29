package io.smallrye.reactive.adapt.converters;

import java.util.function.Function;

import io.smallrye.reactive.Uni;
import reactor.core.publisher.Mono;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {
    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(uni.adapt().toPublisher());
    }
}
