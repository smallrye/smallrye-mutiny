package io.smallrye.mutiny.jakarta.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.jakarta.streams.Engine;
import io.smallrye.mutiny.jakarta.streams.operators.TerminalStage;
import io.smallrye.mutiny.jakarta.streams.operators.TerminalStageFactory;
import io.smallrye.mutiny.jakarta.streams.utils.WrappedSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of the {@link Stage.SubscriberStage} stage.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class SubscriberStageFactory implements TerminalStageFactory<Stage.SubscriberStage> {

    @SuppressWarnings("unchecked")
    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.SubscriberStage stage) {
        Subscriber<I> subscriber = (Subscriber<I>) Objects.requireNonNull(stage).getRsSubscriber();
        Objects.requireNonNull(subscriber);
        return (TerminalStage<I, O>) new SubscriberStage<>(subscriber);
    }

    private static class SubscriberStage<I> implements TerminalStage<I, Void> {

        private final Subscriber<I> subscriber;

        SubscriberStage(Subscriber<I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public CompletionStage<Void> apply(Multi<I> source) {
            WrappedSubscriber<I> wrapped = new WrappedSubscriber<>(subscriber);
            source.subscribe().withSubscriber(AdaptersToFlow.subscriber(wrapped));
            return wrapped.future();
        }
    }

}
