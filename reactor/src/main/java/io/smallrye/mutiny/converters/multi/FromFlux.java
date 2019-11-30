package io.smallrye.mutiny.converters.multi;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;
import reactor.core.publisher.Flux;

public class FromFlux<T> implements MultiConverter<Flux<T>, T> {

    public final static FromFlux INSTANCE = new FromFlux();

    private FromFlux() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Flux<T> instance) {
        return Multi.createFrom().publisher(instance);
    }
}
