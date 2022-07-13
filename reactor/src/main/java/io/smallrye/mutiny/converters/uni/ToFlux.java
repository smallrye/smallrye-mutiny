package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import reactor.core.publisher.Flux;

public class ToFlux<T> implements Function<Uni<T>, Flux<T>> {

    public final static ToFlux INSTANCE = new ToFlux();

    private ToFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Flux<T> apply(Uni<T> uni) {
        return Flux.from(AdaptersToReactiveStreams.publisher(uni.convert().toPublisher()));
    }
}
