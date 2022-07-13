package io.smallrye.mutiny.streams;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.eclipse.microprofile.reactive.streams.operators.spi.SubscriberWithCompletionStage;
import org.eclipse.microprofile.reactive.streams.operators.spi.UnsupportedStageException;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.operators.Operator;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessorOperator;
import io.smallrye.mutiny.streams.operators.PublisherOperator;
import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.streams.operators.TerminalOperator;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.spi.Transformer;
import io.smallrye.mutiny.streams.stages.Stages;
import io.smallrye.mutiny.streams.utils.ConnectableProcessor;
import io.smallrye.mutiny.streams.utils.DefaultSubscriberWithCompletionStage;
import io.smallrye.mutiny.streams.utils.WrappedProcessor;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class Engine implements ReactiveStreamsEngine {

    @Override
    public <T> Publisher<T> buildPublisher(Graph graph) {
        Multi<T> publisher = null;
        for (Stage stage : graph.getStages()) {
            Operator operator = Stages.lookup(stage);
            if (publisher == null) {
                if (operator instanceof PublisherOperator) {
                    publisher = createPublisher(stage, (PublisherOperator) operator);
                } else {
                    throw new IllegalArgumentException("Expecting a publisher stage, got a " + stage);
                }
            } else {
                if (operator instanceof ProcessorOperator) {
                    publisher = applyProcessors(publisher, stage, (ProcessorOperator) operator);
                } else {
                    throw new IllegalArgumentException("Expecting a processor stage, got a " + stage);
                }
            }
        }
        return AdaptersToReactiveStreams.publisher(publisher);
    }

    @Override
    public <T, R> SubscriberWithCompletionStage<T, R> buildSubscriber(Graph graph) {
        Processor<T, T> processor = new ConnectableProcessor<>();
        Multi<T> flowable = Multi.createFrom().publisher(AdaptersToFlow.publisher(processor));
        for (Stage stage : graph.getStages()) {
            Operator operator = Stages.lookup(stage);
            if (operator instanceof ProcessorOperator) {
                flowable = applyProcessors(flowable, stage, (ProcessorOperator) operator);
            } else if (operator instanceof TerminalOperator) {
                CompletionStage<R> result = applySubscriber(Transformer.apply(flowable), stage,
                        (TerminalOperator) operator);
                return new DefaultSubscriberWithCompletionStage<>(processor, result);
            } else {
                throw new UnsupportedStageException(stage);
            }
        }

        throw new IllegalArgumentException("The graph does not have a valid final stage");
    }

    @Override
    public <T, R> Processor<T, R> buildProcessor(Graph graph) {
        Processor<T, T> processor = new ConnectableProcessor<>();

        Multi<T> multi = Multi.createFrom().publisher(AdaptersToFlow.publisher(processor));
        for (Stage stage : graph.getStages()) {
            Operator operator = Stages.lookup(stage);
            multi = applyProcessors(multi, stage, (ProcessorOperator) operator);
        }

        //noinspection unchecked
        return (Processor<T, R>) new WrappedProcessor<>(processor, AdaptersToReactiveStreams.publisher(multi));
    }

    @Override
    public <T> CompletionStage<T> buildCompletion(Graph graph) {
        Multi<?> flowable = null;
        for (Stage stage : graph.getStages()) {
            Operator operator = Stages.lookup(stage);
            if (operator instanceof PublisherOperator) {
                flowable = createPublisher(stage, (PublisherOperator) operator);
            } else if (operator instanceof ProcessorOperator) {
                flowable = applyProcessors(flowable, stage, (ProcessorOperator) operator);
            } else {
                return applySubscriber(flowable, stage, (TerminalOperator) operator);
            }
        }

        throw new IllegalArgumentException("Graph did not have terminal stage");
    }

    private <I, O> Multi<O> applyProcessors(Multi<I> multi, Stage stage, ProcessorOperator operator) {
        @SuppressWarnings("unchecked")
        ProcessingStage<I, O> ps = operator.create(this, stage);
        return Transformer.apply(ps.apply(multi));
    }

    private <T, R> CompletionStage<R> applySubscriber(Multi<T> multi, Stage stage, TerminalOperator operator) {
        @SuppressWarnings("unchecked")
        TerminalStage<T, R> ps = operator.create(this, stage);
        return ps.apply(Transformer.apply(multi));
    }

    private <O> Multi<O> createPublisher(Stage stage, PublisherOperator operator) {
        @SuppressWarnings("unchecked")
        PublisherStage<O> ps = operator.create(this, stage);
        return Transformer.apply(ps.get());
    }

}
