package io.smallrye.mutiny.converters.uni;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import reactor.core.publisher.Mono;

public class FromMono<T> implements UniConverter<Mono<T>, T> {

    public final static FromMono INSTANCE = new FromMono();

    private FromMono() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Mono<T> instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance));
    }
}
