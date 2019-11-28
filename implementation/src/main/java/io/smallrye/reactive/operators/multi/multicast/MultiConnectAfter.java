package io.smallrye.reactive.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.MultiOperator;

/**
 * A {@link Multi} subscribing upstream when a number of subscribers is reached.
 * 
 * @param <T> the type of item.
 */
public class MultiConnectAfter<T> extends MultiOperator<T, T> {
    private final int numberOfSubscribers;
    private final AtomicInteger count = new AtomicInteger();
    private final ConnectableMultiConnection connection;

    public MultiConnectAfter(ConnectableMulti<T> upstream,
            int numberOfSubscribers,
            ConnectableMultiConnection connection) {
        super(upstream);
        this.numberOfSubscribers = numberOfSubscribers;
        this.connection = connection;
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        // TODO Wondering if we can just delay the subscription and not call connect.
        upstream().subscribe(downstream);
        if (count.incrementAndGet() == numberOfSubscribers) {
            ((ConnectableMulti) upstream()).connect(connection);
        }
    }
}
