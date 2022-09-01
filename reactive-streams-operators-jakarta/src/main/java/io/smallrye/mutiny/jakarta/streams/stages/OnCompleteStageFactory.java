package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStage;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessingStageFactory;

/**
 * Implementation of the {@link Stage.OnComplete} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnCompleteStageFactory implements ProcessingStageFactory<Stage.OnComplete> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.OnComplete stage) {
        Runnable runnable = Objects.requireNonNull(stage).getAction();
        Objects.requireNonNull(runnable);
        return source -> (Multi<O>) source.onCompletion().invoke(runnable);
    }
}
