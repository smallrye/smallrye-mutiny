package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.test.AssertSubscriber.DEFAULT_TIMEOUT;
import static io.smallrye.mutiny.helpers.test.AssertionHelper.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * A {@link io.smallrye.mutiny.Uni} {@link UniSubscriber} for testing purposes that comes with useful assertion helpers.
 *
 * @param <T> the type of the items
 */
public class UniAssertSubscriber<T> implements UniSubscriber<T> {
    private volatile boolean cancelImmediatelyOnSubscription;
    private final Context context;

    // Writable from the subscribers
    private final CompletableFuture<T> completion = new CompletableFuture<>();
    private final CompletableFuture<UniSubscription> subscribed = new CompletableFuture<>();

    // Readable from the assertions
    private final CompletableFuture<T> hasCompleted;
    private final CompletableFuture<UniSubscription> hasSubscription;

    private volatile UniSubscription subscription;
    private volatile T item;
    private volatile Throwable failure;
    private volatile boolean hasCompletedSuccessfully;
    private volatile String onResultThreadName;
    private volatile String onErrorThreadName;
    private volatile String onSubscribeThreadName;

    private final List<UniSignal> signals = new ArrayList<>(4);

    /**
     * Create a new {@link UniAssertSubscriber}.
     *
     * @param context the subscription context, cannot be {@code null}
     * @param cancelled {@code true} when the subscription shall be cancelled upfront, {@code false} otherwise
     */
    public UniAssertSubscriber(Context context, boolean cancelled) {
        this.context = nonNull(context, "context");
        hasCompleted = completion.whenComplete((item, failure) -> {
            if (failure == null) {
                this.onResultThreadName = Thread.currentThread().getName();
                this.hasCompletedSuccessfully = true;
                this.item = item;
            } else {
                this.onErrorThreadName = Thread.currentThread().getName();
                this.failure = failure;
            }
        }).toCompletableFuture();
        hasSubscription = subscribed.thenApply(s -> {
            this.onSubscribeThreadName = Thread.currentThread().getName();
            this.subscription = s;
            return s;
        }).toCompletableFuture();

        this.cancelImmediatelyOnSubscription = cancelled;
    }

    /**
     * Create a new {@link UniAssertSubscriber} with an upfront cancellation configuration and an empty {@link Context}.
     */
    public UniAssertSubscriber(boolean cancelled) {
        this(Context.empty(), cancelled);
    }

    /**
     * Create a new {@link UniAssertSubscriber} with no upfront cancellation and an empty {@link Context}.
     */
    public UniAssertSubscriber() {
        this(false);
    }

    /**
     * Create a new {@link UniAssertSubscriber} with no upfront cancellation and an empty {@link Context}.
     *
     * @param <T> the type of the item
     * @return a new subscriber
     */
    public static <T> UniAssertSubscriber<T> create() {
        return new UniAssertSubscriber<>();
    }

    /**
     * Create a new {@link UniAssertSubscriber} with no upfront cancellation and a {@link Context}.
     *
     * @param <T> the type of the item
     * @param context the context, cannot be {@code null}
     * @return a new subscriber
     */
    public static <T> UniAssertSubscriber<T> create(Context context) {
        return new UniAssertSubscriber<>(context, false);
    }

    @Override
    public Context context() {
        return this.context;
    }

    @Override
    public synchronized void onSubscribe(UniSubscription subscription) {
        signals.add(new OnSubscribeUniSignal(subscription));
        subscribed.complete(subscription);
        if (this.cancelImmediatelyOnSubscription) {
            subscription.cancel();
            completion.cancel(false);
        }
    }

    @Override
    public synchronized void onItem(T item) {
        signals.add(new OnItemUniSignal<>(item));
        this.completion.complete(item);
    }

    @Override
    public synchronized void onFailure(Throwable failure) {
        signals.add(new OnFailureUniSignal(failure));
        this.completion.completeExceptionally(failure);
    }

    /**
     * Awaits for an item event.
     * It waits at most {@link AssertSubscriber#DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if a failure event is received instead of the expected completion, the check fails.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitItem() {
        return awaitItem(DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a item event at most {@code duration}.
     * <p>
     * If the timeout expired, or if a failure event is received instead of the expected completion, the check fails.
     *
     * @param duration the duration, must not be {@code null}
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitItem(Duration duration) {
        try {
            awaitEvent(hasCompleted, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "No item (or failure) event received in the last " + duration.toMillis() + " ms");
        }

        if (failure == null) {
            return this;
        } else {
            throw new AssertionError("Expected an item event but got a failure: " + failure);
        }
    }

    /**
     * Awaits for a failure event.
     * It waits at most {@link AssertSubscriber#DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if an item event is received instead of the expected failure, the check fails.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitFailure() {
        return awaitFailure(t -> {
        });
    }

    /**
     * Awaits for a failure event and validate it.
     * It waits at most {@link AssertSubscriber#DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if an item event is received instead of the expected failure, the check fails.
     * The received failure is validated using the {@code assertion} consumer. The code of the consumer is expected to
     * throw an {@link AssertionError} to indicate that the failure didn't pass the validation. The consumer is not
     * called if no failures are received.
     *
     * @param assertion a check validating the received failure (if any). Must not be {@code null}
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitFailure(Consumer<Throwable> assertion) {
        return awaitFailure(assertion, DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a failure event.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, or if an item event is received instead of the expected failure, the check fails.
     *
     * @param duration the max duration to wait, must not be {@code null}
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitFailure(Duration duration) {
        return awaitFailure(t -> {
        }, duration);
    }

    /**
     * Awaits for a failure event and validate it.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, or if an item event is received instead of the expected failure, the check fails.
     * The received failure is validated using the {@code assertion} consumer. The code of the consumer is expected to
     * throw an {@link AssertionError} to indicate that the failure didn't pass the validation. The consumer is not
     * called if no failures are received.
     *
     * @param assertion a check validating the received failure (if any). Must not be {@code null}
     * @param duration the max duration to wait, must not be {@code null}
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitFailure(Consumer<Throwable> assertion, Duration duration) {
        try {
            awaitEvent(hasCompleted, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "No item (or failure) event received in the last " + duration.toMillis() + " ms");
        }

        if (failure == null) {
            throw new AssertionError("Expected a failure event but got an item event: " + item);
        }

        try {
            assertion.accept(failure);
            return this;
        } catch (AssertionError e) {
            throw new AssertionError("Received a failure event, but that failure did not pass the validation: " + e, e);
        }

    }

    /**
     * Awaits for a subscription event (the subscriber receives a {@link UniSubscription} from the upstream.
     * It waits at most {@link AssertSubscriber#DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, the check fails.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitSubscription() {
        return awaitSubscription(DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a subscription event (the subscriber receives a {@link UniSubscription} from the upstream.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, the check fails.
     *
     * @param duration the UniAssertSubscriber, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public UniAssertSubscriber<T> awaitSubscription(Duration duration) {
        try {
            awaitEvent(hasSubscription, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "Expecting a subscription event in the last " + duration.toMillis() + " ms, but did not get it");
        }
        return this;
    }

    private void awaitEvent(CompletableFuture<?> future, Duration duration) throws TimeoutException {
        // Are we already done?
        if (future.isDone()) {
            return;
        }
        try {
            future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Completed exceptionally, but completed anyway.
        }
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has completed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public synchronized UniAssertSubscriber<T> assertCompleted() {
        shouldHaveCompleted(hasCompletedSuccessfully, failure, null);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public synchronized UniAssertSubscriber<T> assertFailed() {
        shouldHaveFailed(hasCompletedSuccessfully, failure, null, null);
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
        if (failure instanceof CancellationException) {
            return null;
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
        shouldHaveCompleted(hasCompletedSuccessfully, failure, null);
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
    public UniAssertSubscriber<T> assertFailedWith(Class<? extends Throwable> expectedTypeOfFailure,
            String expectedMessage) {
        shouldHaveFailed(hasCompletedSuccessfully, failure, expectedTypeOfFailure, expectedMessage);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has failed.
     *
     * @param expectedTypeOfFailure the expected failure type
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertFailedWith(Class<? extends Throwable> expectedTypeOfFailure) {
        shouldHaveFailed(hasCompletedSuccessfully, failure, expectedTypeOfFailure, null);
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
        shouldBeTerminated(hasCompletedSuccessfully, failure);
        return this;
    }

    /**
     * Assert that the {@link io.smallrye.mutiny.Uni} has not terminated.
     *
     * @return this {@link UniAssertSubscriber}
     */
    public UniAssertSubscriber<T> assertNotTerminated() {
        shouldNotBeTerminatedUni(hasCompletedSuccessfully, failure);
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
