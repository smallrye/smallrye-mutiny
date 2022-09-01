package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;

import io.smallrye.mutiny.jakarta.streams.operators.Operator;
import io.smallrye.mutiny.jakarta.streams.operators.ProcessorOperator;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherOperator;
import io.smallrye.mutiny.jakarta.streams.operators.TerminalOperator;

/**
 * Allows looking for the {@link Operator} for a given {@link Stage}.
 */
public class Stages {

    private static final List<Operator> ALL;

    static {
        ALL = new ArrayList<>();

        ALL.add(new ProcessorOperator<>(Stage.Distinct.class, new DistinctStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Filter.class, new FilterStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.FlatMap.class, new FlatMapStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.FlatMapCompletionStage.class, new FlatMapCompletionStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.FlatMapIterable.class, new FlatMapIterableStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Map.class, new MapStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Peek.class, new PeekStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.OnComplete.class, new OnCompleteStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.OnTerminate.class, new OnTerminateStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.OnError.class, new OnErrorStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.OnErrorResume.class, new OnErrorResumeStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.OnErrorResumeWith.class, new OnErrorResumeWithStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.ProcessorStage.class, new ProcessorStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.TakeWhile.class, new TakeWhileStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.DropWhile.class, new DropWhileStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Limit.class, new LimitStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Skip.class, new SkipStageFactory()));
        ALL.add(new ProcessorOperator<>(Stage.Coupled.class, new CoupledStageFactory()));

        ALL.add(new PublisherOperator<>(Stage.Concat.class, new ConcatStageFactory()));
        ALL.add(new PublisherOperator<>(Stage.Failed.class, new FailedPublisherStageFactory()));
        ALL.add(new PublisherOperator<>(Stage.Of.class, new FromIterableStageFactory()));
        ALL.add(new PublisherOperator<>(Stage.PublisherStage.class, new FromPublisherStageFactory()));
        ALL.add(new PublisherOperator<>(Stage.FromCompletionStage.class, new FromCompletionStageFactory()));
        ALL.add(new PublisherOperator<>(Stage.FromCompletionStageNullable.class,
                new FromCompletionStageNullableFactory()));

        ALL.add(new TerminalOperator<>(Stage.Cancel.class, new CancelStageFactory()));
        ALL.add(new TerminalOperator<>(Stage.Collect.class, new CollectStageFactory()));
        ALL.add(new TerminalOperator<>(Stage.FindFirst.class, new FindFirstStageFactory()));
        ALL.add(new TerminalOperator<>(Stage.SubscriberStage.class, new SubscriberStageFactory()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Stage> Operator<T> lookup(T stage) {
        Objects.requireNonNull(stage, "The stage must not be `null`");
        return ALL.stream().filter(p -> p.test(stage)).findAny()
                .orElseThrow(() -> new UnsupportedStageException(stage));
    }

    private Stages() {
        // Avoid direct instantiation.
    }

}
