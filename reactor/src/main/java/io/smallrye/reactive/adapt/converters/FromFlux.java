package io.smallrye.reactive.adapt.converters;

import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.UniConverter;
import reactor.core.publisher.Flux;

public class FromFlux<T> implements UniConverter<Flux<T>, T> {

    public final static FromFlux INSTANCE = new FromFlux();

    private FromFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Flux<T> instance) {
        return Uni.createFrom().publisher(instance);
    }
}
