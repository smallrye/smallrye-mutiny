package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.TerminalStage;
import io.smallrye.mutiny.streams.operators.TerminalStageFactory;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Implementation of {@link Stage.Cancel}. It subscribes and disposes the stream immediately.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class CancelStageFactory implements TerminalStageFactory<Stage.Cancel> {

    @Override
    public <I, O> TerminalStage<I, O> create(Engine engine, Stage.Cancel stage) {
        Objects.requireNonNull(stage);
        return (Multi<I> flow) -> {
            //noinspection SubscriberImplementation
            flow.subscribe(AdaptersToFlow.subscriber(new Subscriber<I>() {
                @Override
                public void onSubscribe(Subscription s) {
                    s.cancel();
                }

                @Override
                public void onNext(I in) {
                    // Do nothing.
                }

                @Override
                public void onError(Throwable t) {
                    // Do nothing.
                }

                @Override
                public void onComplete() {
                    // Do nothing.
                }
            }));
            return Infrastructure.wrapCompletableFuture(CompletableFuture.completedFuture(null));
        };
    }
}
