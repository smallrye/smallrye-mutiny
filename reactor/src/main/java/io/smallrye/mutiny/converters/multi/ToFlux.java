package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import reactor.core.publisher.Flux;

public class ToFlux<T> implements Function<Multi<T>, Flux<T>> {

    public final static ToFlux INSTANCE = new ToFlux();

    private ToFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Flux<T> apply(Multi<T> multi) {
        return Flux.from(AdaptersToReactiveStreams.publisher(multi));
    }
}
