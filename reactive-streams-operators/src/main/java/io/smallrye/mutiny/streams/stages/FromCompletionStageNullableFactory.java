package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.streams.operators.PublisherStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;

public class FromCompletionStageNullableFactory implements PublisherStageFactory<Stage.FromCompletionStageNullable> {

    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.FromCompletionStageNullable stage) {
        Objects.requireNonNull(stage);
        return () -> {
            CompletionStage<O> cs = Casts.cast(Objects.requireNonNull(stage.getCompletionStage()));
            return Multi.createFrom().completionStage(cs);
        };
    }

}
