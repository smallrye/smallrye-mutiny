package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStage;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStageFactory;
import io.smallrye.mutiny.jakarta.streams.utils.Casts;

public class FromCompletionStageFactory implements PublisherStageFactory<Stage.FromCompletionStage> {

    @Override
    public <O> PublisherStage<O> create(Engine engine, Stage.FromCompletionStage stage) {
        Objects.requireNonNull(stage);
        return () -> {
            CompletionStage<O> cs = Casts.cast(Objects.requireNonNull(stage.getCompletionStage()));
            return Multi.createFrom().completionStage(cs)
                    .onCompletion().ifEmpty().failWith(NullPointerException::new);
        };
    }

}
