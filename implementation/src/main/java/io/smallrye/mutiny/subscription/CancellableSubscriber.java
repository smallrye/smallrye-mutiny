package io.smallrye.mutiny.subscription;

public interface CancellableSubscriber<T> extends MultiSubscriber<T>, Cancellable {
}
