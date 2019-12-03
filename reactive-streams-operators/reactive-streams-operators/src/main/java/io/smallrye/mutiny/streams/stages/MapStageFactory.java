package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;

/**
 * Implementation of the {@link Stage.Map} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MapStageFactory implements ProcessingStageFactory<Stage.Map> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.Map stage) {
        Function<I, O> mapper = Casts.cast(stage.getMapper());
        Objects.requireNonNull(mapper);
        return source -> source.map(mapper);
    }
}
