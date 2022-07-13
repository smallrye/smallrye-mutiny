package io.smallrye.mutiny.converters.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import reactor.core.publisher.Mono;

public class FromMono<T> implements MultiConverter<Mono<T>, T> {

    public final static FromMono INSTANCE = new FromMono();

    private FromMono() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Mono<T> instance) {
        return Multi.createFrom().publisher(AdaptersToFlow.publisher(instance));
    }
}
