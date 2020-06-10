package io.smallrye.mutiny.streams.stages;

import static io.smallrye.mutiny.helpers.ParameterValidation.SUPPLIER_PRODUCED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.MultiFlatten;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;
import io.smallrye.mutiny.subscription.BackPressureStrategy;

/**
 * Implementation of the {@link Stage.FlatMapCompletionStage} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapCompletionStageFactory
        implements ProcessingStageFactory<Stage.FlatMapCompletionStage> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine,
            Stage.FlatMapCompletionStage stage) {
        Function<I, CompletionStage<O>> mapper = Casts.cast(
                Objects.requireNonNull(stage).getMapper());
        return new FlatMapCompletionStage<>(mapper);
    }

    private static class FlatMapCompletionStage<I, O> implements ProcessingStage<I, O> {
        private final Function<I, CompletionStage<O>> mapper;

        private FlatMapCompletionStage(Function<I, CompletionStage<O>> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
        }

        @Override
        public Multi<O> apply(Multi<I> source) {
            // We cannot use applyCompletionStage as the Reactive Streams Operators TCK expect to fail if the completion
            // stage produces a `null` value, which is not the case with applyCompletionStage

            nonNull(mapper, "mapper");
            Function<? super I, ? extends Publisher<? extends O>> wrapper = res -> {
                Objects.requireNonNull(res);
                return Multi.createFrom().emitter(emitter -> {
                    CompletionStage<? extends O> stage;
                    try {
                        stage = mapper.apply(res);
                    } catch (Throwable e) {
                        emitter.fail(e);
                        return;
                    }
                    if (stage == null) {
                        emitter.fail(new NullPointerException(SUPPLIER_PRODUCED_NULL));
                        return;
                    }

                    emitter.onTermination(() -> stage.toCompletableFuture().cancel(false));
                    stage.whenComplete((r, f) -> {
                        if (f != null) {
                            emitter.fail(f);
                        } else if (r != null) {
                            emitter.emit(r);
                            emitter.complete();
                        } else {
                            // Here is the difference with the applyCompletionStage mehtod.
                            emitter.fail(new NullPointerException("The completion stage produced a `null` value"));
                        }
                    });
                }, BackPressureStrategy.LATEST);
            };
            return new MultiFlatten<>(source, wrapper, 1, false).concatenate();
        }
    }

}
