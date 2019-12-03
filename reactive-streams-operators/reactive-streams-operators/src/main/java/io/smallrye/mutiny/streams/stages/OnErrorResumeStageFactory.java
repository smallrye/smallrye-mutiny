package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.OnErrorResume} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnErrorResumeStageFactory implements ProcessingStageFactory<Stage.OnErrorResume> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.OnErrorResume stage) {
        Function<Throwable, I> function = (Function<Throwable, I>) Objects.requireNonNull(stage).getFunction();
        Objects.requireNonNull(function);
        return source -> (Multi<O>) source.onFailure().recoverWithItem(function);
    }
}
