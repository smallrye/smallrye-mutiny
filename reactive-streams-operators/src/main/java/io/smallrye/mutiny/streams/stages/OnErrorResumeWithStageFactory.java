package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.OnErrorResumeWith} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class OnErrorResumeWithStageFactory implements ProcessingStageFactory<Stage.OnErrorResumeWith> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.OnErrorResumeWith stage) {
        Function<Throwable, Graph> function = Objects.requireNonNull(stage).getFunction();
        Objects.requireNonNull(function);

        return source -> (Multi<O>) source.onFailure().recoverWithMulti(failure -> {
            Graph graph = function.apply(failure);
            Publisher<I> publisher = engine.buildPublisher(Objects.requireNonNull(graph));
            return Multi.createFrom().publisher(AdaptersToFlow.publisher(publisher));
        });
    }
}
