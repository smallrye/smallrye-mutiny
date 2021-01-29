package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.test.AssertionHelper.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private volatile T item;
    private volatile Throwable failure;
    private volatile boolean completed;
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private volatile String onResultThreadName;
    private volatile String onErrorThreadName;
    private volatile String onSubscribeThreadName;
    private final List<UniSignal> signals = new ArrayList<>(4);

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
        signals.add(new OnSubscribeUniSignal(subscription));
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
        signals.add(new OnItemUniSignal(item));
        this.completed = true;
        this.item = item;
        this.onResultThreadName = Thread.currentThread().getName();
        this.future.complete(item);
    }

    @Override
    public synchronized void onFailure(Throwable failure) {
        signals.add(new OnFailureUniSignal(failure));
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
        shouldHaveCompleted(completed, failure, null);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public synchronized UniAssertSubscriber<T> assertFailed() {
        shouldHaveFailed(completed, failure, null, null);
        return this;
    }

    /**
     * Get the {@link io.smallrye.mutiny.Uni} item, if any.
     *
     * @return the item or {@code null}
     */
    public synchronized T getItem() {
        return item;
    }

    /**
     * Get the {@link io.smallrye.mutiny.Uni} failure, if any.
     *
     * @return the failure or {@code null}
     */
    public synchronized Throwable getFailure() {
        return failure;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has received an item.
     *
     * @param expected the expected item
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertItem(T expected) {
        shouldHaveCompleted(completed, failure, null);
        shouldHaveReceived(getItem(), expected);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @param expectedTypeOfFailure the expected failure type
     * @param expectedMessage a message that is expected to be contained in the failure message
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertFailedWith(Class<? extends Throwable> expectedTypeOfFailure, String expectedMessage) {
        shouldHaveFailed(completed, failure, expectedTypeOfFailure, expectedMessage);
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
        signals.add(new OnCancellationUniSignal());
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
        shouldBeTerminated(completed, failure);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has not terminated.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertNotTerminated() {
        shouldNotBeTerminated(completed, failure);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has been subscribed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertSubscribed() {
        shouldBeSubscribed(subscription == null ? 0 : 1);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has not been subscribed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertNotSubscribed() {
        shouldNotBeSubscribed(subscription == null ? 0 : 1);
        return this;
    }

    /**
     * Get the {@link UniSignal} audit trail for this subscriber.
     * 
     * @return the signals in receive order
     */
    public List<UniSignal> getSignals() {
        return Collections.unmodifiableList(signals);
    }

    /**
     * Assert that signals have been received in correct order.
     * <p>
     * An example of an legal sequence would be receiving {@code onSubscribe -> onItem}.
     * An example of an illegal sequence would be receiving {@code onItem -> onSubscribe}.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertSignalsReceivedInOrder() {
        if (signals.isEmpty()) {
            return this;
        }
        UniSignal firstSignal = signals.get(0);
        if (!(firstSignal instanceof OnSubscribeUniSignal)
                && !(firstSignal instanceof OnCancellationUniSignal)) {
            throw new AssertionError("The first signal is neither onSubscribe nor cancel but " + firstSignal);
        }
        int[] occurrences = new int[3]; // onSubscribe | onItem | onFailure
        for (UniSignal signal : signals) {
            if (signal instanceof OnSubscribeUniSignal) {
                occurrences[0]++;
            } else if (signal instanceof OnItemUniSignal) {
                occurrences[1]++;
            } else if (signal instanceof OnFailureUniSignal) {
                occurrences[2]++;
            }
        }
        if (occurrences[0] > 1) {
            throw new AssertionError("There are more than 1 onSubscribe signals in " + signals);
        }
        if (occurrences[1] > 1) {
            throw new AssertionError("There are more than 1 onItem signals in " + signals);
        }
        if (occurrences[2] > 1) {
            throw new AssertionError("There are more than 1 onFailure signals in " + signals);
        }
        if (occurrences[1] == 1 && occurrences[2] == 1) {
            throw new AssertionError("There are both onItem and onFailure signals in " + signals);
        }
        return this;
    }
}
