package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import reactor.core.publisher.Mono;

public class ToMono<T> implements Function<Uni<T>, Mono<T>> {

    public final static ToMono INSTANCE = new ToMono();

    private ToMono() {
        // Avoid direct instantiation
    }

    @Override
    public Mono<T> apply(Uni<T> uni) {
        return Mono.from(AdaptersToReactiveStreams.publisher(uni.convert().toPublisher()));
    }
}
