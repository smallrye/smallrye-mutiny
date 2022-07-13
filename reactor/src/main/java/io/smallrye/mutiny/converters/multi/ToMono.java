package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import reactor.core.publisher.Mono;

public class ToMono<T> implements Function<Multi<T>, Mono<T>> {

    public final static ToMono INSTANCE = new ToMono();

    private ToMono() {
        // Avoid direct instantiation
    }

    @Override
    public Mono<T> apply(Multi<T> multi) {
        return Mono.from(AdaptersToReactiveStreams.publisher(multi));
    }
}
