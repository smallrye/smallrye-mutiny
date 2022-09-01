package io.smallrye.mutiny.jakarta.streams.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.jakarta.streams.stages.ConcatStageFactory;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

/**
 * Wrapped a source publisher and make it cancellable on demand. The cancellation happens if
 * no-one subscribed to the source publisher. This class is required for the
 * {@link ConcatStageFactory} to enforce the reactive
 * streams rules.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@SuppressWarnings("PublisherImplementation")
public class CancellablePublisher<T> implements Publisher<T> {
    private final Publisher<T> source;
    private final AtomicBoolean subscribed = new AtomicBoolean();

    public CancellablePublisher(Publisher<T> delegate) {
        this.source = delegate;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber);
        if (subscribed.compareAndSet(false, true)) {
            source.subscribe(subscriber);
        } else {
            Subscriptions.fail(AdaptersToFlow.subscriber(subscriber), new IllegalStateException("Multicast not supported"));
        }
    }

    public void cancelIfNotSubscribed() {
        if (subscribed.compareAndSet(false, true)) {
            source.subscribe(AdaptersToReactiveStreams.subscriber(new Subscriptions.CancelledSubscriber<>()));
        }
    }
}
