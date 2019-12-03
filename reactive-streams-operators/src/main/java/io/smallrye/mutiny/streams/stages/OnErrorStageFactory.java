package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.OnError} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnErrorStageFactory implements ProcessingStageFactory<Stage.OnError> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.OnError stage) {
        Consumer<Throwable> consumer = Objects.requireNonNull(stage).getConsumer();
        Objects.requireNonNull(consumer);
        return source -> (Multi<O>) source.onFailure().invoke(consumer);
    }
}
