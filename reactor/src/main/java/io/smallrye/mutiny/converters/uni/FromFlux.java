package io.smallrye.mutiny.converters.uni;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import reactor.core.publisher.Flux;

public class FromFlux<T> implements UniConverter<Flux<T>, T> {

    public final static FromFlux INSTANCE = new FromFlux();

    private FromFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Flux<T> instance) {
        return Uni.createFrom().publisher(AdaptersToFlow.publisher(instance));
    }
}
