package io.smallrye.mutiny.streams.stages;

import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Processor;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.ProcessorStage} stage ({@code via} operators).
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class ProcessorStageFactory implements ProcessingStageFactory<Stage.ProcessorStage> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.ProcessorStage stage) {
        Processor<I, O> processor = Casts.cast(Objects.requireNonNull(
                Objects.requireNonNull(stage).getRsProcessor()));
        return source -> Multi.createFrom().deferred(() -> {
            Multi<O> multi = Multi.createFrom().publisher(AdaptersToFlow.publisher(processor));
            source.subscribe().withSubscriber(AdaptersToFlow.processor(processor));
            ;
            return multi;
        });
    }
}
