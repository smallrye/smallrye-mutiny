package io.smallrye.reactive.converters.uni;

import java.util.function.Function;

import io.smallrye.reactive.Uni;
import reactor.core.publisher.Mono;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {

    public final static ToMono INSTANCE = new ToMono();

    private ToMono() {
        // Avoid direct instantiation
    }

    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(uni.convert().toPublisher());
    }
}
