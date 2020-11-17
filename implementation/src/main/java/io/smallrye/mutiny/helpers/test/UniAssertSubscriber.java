package io.smallrye.mutiny.helpers.test;

import java.util.concurrent.CompletableFuture;

import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * A {@link io.smallrye.mutiny.Uni} {@link UniSubscriber} for testing purposes that comes with useful assertion helpers.
 *
 * @param <T> the type of the items
 */
public class UniAssertSubscriber<T> implements UniSubscriber<T> {
    private volatile boolean cancelImmediatelyOnSubscription;
    private volatile UniSubscription subscription;
    private volatile boolean gotSignal;
    private volatile T item;
    private volatile Throwable failure;
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private volatile String onResultThreadName;
    private volatile String onErrorThreadName;
    private volatile String onSubscribeThreadName;

    /**
     * Create a new {@link UniAssertSubscriber}.
     *
     * @param cancelled {@code true} when the subscription shall be cancelled upfront, {@code false} otherwise
     */
    public UniAssertSubscriber(boolean cancelled) {
        this.cancelImmediatelyOnSubscription = cancelled;
    }

    /**
     * Create a new {@link UniAssertSubscriber} with no upfront cancellation.
     */
    public UniAssertSubscriber() {
        this(false);
    }

    /**
     * Create a new {@link UniAssertSubscriber} with no upfront cancellation.
     * 
     * @param <T> the type of the item
     * @return a new subscriber
     */
    public static <T> UniAssertSubscriber<T> create() {
        return new UniAssertSubscriber<>();
    }

    @Override
    public synchronized void onSubscribe(UniSubscription subscription) {
        onSubscribeThreadName = Thread.currentThread().getName();
        if (this.cancelImmediatelyOnSubscription) {
            this.subscription = subscription;
            subscription.cancel();
            future.cancel(false);
            return;
        }
        this.subscription = subscription;
    }

    @Override
    public synchronized void onItem(T item) {
        this.gotSignal = true;
        this.item = item;
        this.onResultThreadName = Thread.currentThread().getName();
        this.future.complete(item);
    }

    @Override
    public synchronized void onFailure(Throwable failure) {
        this.gotSignal = true;
        this.failure = failure;
        this.onErrorThreadName = Thread.currentThread().getName();
        this.future.completeExceptionally(failure);
    }

    /**
     * Await for termination.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> await() {
        CompletableFuture<T> fut;
        synchronized (this) {
            fut = this.future;
        }
        try {
            fut.join();
        } catch (Exception e) {
            // Error already caught.
        }
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has completed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public synchronized UniAssertSubscriber<T> assertCompleted() {
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }

        if (future.isCompletedExceptionally()) {
            throw new AssertionError("The uni didn't completed successfully: " + failure);
        }
        if (future.isCancelled()) {
            throw new AssertionError("The uni didn't completed successfully, it was cancelled");
        }
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public synchronized UniAssertSubscriber<T> assertFailed() {
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }

        if (!future.isCompletedExceptionally()) {
            throw new AssertionError("The uni completed successfully: " + item);
        }
        if (future.isCancelled()) {
            throw new AssertionError("The uni didn't completed successfully, it was cancelled");
        }
        return this;
    }

    /**
     * Get the {@link io.smallrye.mutiny.Uni} item, if any.
     *
     * @return the item or {@code null}
     */
    public synchronized T getItem() {
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }
        return item;
    }

    /**
     * Get the {@link io.smallrye.mutiny.Uni} failure, if any.
     *
     * @return the failure or {@code null}
     */
    public synchronized Throwable getFailure() {
        if (!this.future.isDone()) {
            throw new IllegalStateException("Not done yet");
        }
        return failure;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has received an item.
     *
     * @param expected the expected item
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertItem(T expected) {
        T item = getItem();
        if (item == null && expected != null) {
            throw new AssertionError("Expected: " + expected + " but was `null`");
        }
        if (item != null && !item.equals(expected)) {
            throw new AssertionError("Expected: " + expected + " but was " + item);
        }
        if (failure != null) {
            throw new AssertionError("Got item and failure " + failure);
        }
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @param typeOfException the expected failure type
     * @param message a message that is expected to be contained in the failure message
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertFailedWith(Class<? extends Throwable> typeOfException, String message) {
        Throwable failure = getFailure();

        if (failure == null) {
            throw new AssertionError("Expected a failure, but the Uni completed with an item");
        }
        if (!typeOfException.isInstance(failure)) {
            throw new AssertionError(
                    "Expected a failure of type " + typeOfException + ", but it was a " + failure.getClass());
        }
        if (!failure.getMessage().contains(message)) {
            throw new AssertionError(
                    "Expected a failure with a message containing '" + message + "', but it was '" + failure
                            .getMessage() + "'");
        }
        return this;
    }

    /**
     * Get the name of the thread that called {@link UniAssertSubscriber#onItem(Object)}, if any.
     *
     * @return the thread name
     */
    public String getOnItemThreadName() {
        return onResultThreadName;
    }

    /**
     * Get the name of the thread that called {@link UniAssertSubscriber#onFailure(Throwable)}, if any.
     *
     * @return the thread name
     */
    public String getOnFailureThreadName() {
        return onErrorThreadName;
    }

    /**
     * Get the name of the thread that called {@link UniAssertSubscriber#onSubscribe(UniSubscription)}, if any.
     *
     * @return the thread name
     */
    public String getOnSubscribeThreadName() {
        return onSubscribeThreadName;
    }

    /**
     * Cancel the subscription.
     */
    public void cancel() {
        if (subscription == null) {
            cancelImmediatelyOnSubscription = true;
        } else {
            subscription.cancel();
        }
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has terminated.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertTerminated() {
        if (gotSignal) {
            return this;
        } else {
            throw new AssertionError("The uni didn't sent a signal");
        }
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has not terminated.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertNotTerminated() {
        if (!gotSignal) {
            return this;
        } else {
            throw new AssertionError("The uni completed");
        }
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has been subscribed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertSubscribed() {
        if (subscription == null) {
            throw new AssertionError("Expected to have a subscription");
        }
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has not been subscribed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertNotSubscribed() {
        if (subscription != null) {
            throw new AssertionError("Expected to not have a subscription");
        }
        return this;
    }
}
