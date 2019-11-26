package io.smallrye.reactive.subscription;

import org.reactivestreams.Subscriber;

public interface CancellableSubscriber<T> extends Subscriber<T>, Cancellable {
}
