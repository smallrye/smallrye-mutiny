package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.test.AssertionHelper.*;
import static java.lang.Integer.parseInt;
import static java.time.Duration.ofSeconds;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A {@link io.smallrye.mutiny.Multi} {@link Subscriber} for testing purposes that comes with useful assertion helpers.
 *
 * @param <T> the type of the items
 */
public class AssertSubscriber<T> implements MultiSubscriber<T>, ContextSupport {

    /**
     * The default timeout used by {@code await} method.
     * <p>
     * This static field is mutable, the authors assume that you know what you are doing if you ever feel like changing
     * its value.
     */
    public static Duration DEFAULT_TIMEOUT;

    /**
     * Name of the environment variable for setting the default await methods timeout (in seconds).
     */
    public static final String DEFAULT_MUTINY_AWAIT_TIMEOUT = "DEFAULT_MUTINY_AWAIT_TIMEOUT";

    static {
        DEFAULT_TIMEOUT = ofSeconds(parseInt(System.getenv().getOrDefault(DEFAULT_MUTINY_AWAIT_TIMEOUT, "10")));
    }

    /**
     * Latch waiting for the completion or failure event.
     */
    private final CountDownLatch terminal = new CountDownLatch(1);

    /**
     * Latch waiting for the subscription event.
     */
    private final CountDownLatch subscribed = new CountDownLatch(1);

    /**
     * The subscription received from upstream.
     */
    private volatile Flow.Subscription subscription = null;

    /**
     * The number of pending requested items.
     */
    private final AtomicLong pendingRequests = new AtomicLong();

    /**
     * The received items.
     */
    private final List<T> items = Collections.synchronizedList(new ArrayList<>());

    /**
     * The received failure.
     */
    private volatile Throwable failure = null;

    /**
     * Number of subscription received from upstream.
     * Reactive Streams compliant upstream should only send one subscription.
     */
    private int numberOfSubscription = 0;

    /**
     * Whether or not the subscriber should cancel the subscription as soon as it receives it.
     * In this case, no request will be made.
     */
    private final boolean upfrontCancellation;

    /**
     * The subscription context.
     */
    private final Context context;

    private enum State {
        INIT,
        SUBSCRIBED,
        FAILED,
        CANCELLED,
        COMPLETED
    }

    private volatile State state = State.INIT;

    /**
     * Creates a new {@link AssertSubscriber}.
     *
     * @param context the context
     * @param requested the number of initially requested items
     * @param cancelled {@code true} if the subscription is immediately cancelled, {@code false} otherwise
     */
    public AssertSubscriber(Context context, long requested, boolean cancelled) {
        this.context = context;
        this.pendingRequests.set(requested);
        this.upfrontCancellation = cancelled;
    }

    /**
     * Creates a new {@link AssertSubscriber}.
     *
     * @param requested the number of initially requested items
     * @param cancelled {@code true} if the subscription is immediately cancelled, {@code false} otherwise
     */
    public AssertSubscriber(long requested, boolean cancelled) {
        this(Context.empty(), requested, cancelled);
    }

    /**
     * Creates a new {@link AssertSubscriber} with 0 requested items and no upfront cancellation.
     */
    public AssertSubscriber() {
        this(Context.empty(), 0, false);
    }

    /**
     * Creates a new {@link AssertSubscriber} with no upfront cancellation.
     *
     * @param requested the number of initially requested items
     */
    public AssertSubscriber(long requested) {
        this(Context.empty(), requested, false);
    }

    /**
     * Creates a new {@link AssertSubscriber} with 0 requested items and no upfront cancellation.
     *
     * @param <T> the items type
     * @return a new subscriber
     */
    public static <T> AssertSubscriber<T> create() {
        return new AssertSubscriber<>(0);
    }

    /**
     * Creates a new {@link AssertSubscriber} with no upfront cancellation.
     *
     * @param requested the number of initially requested items
     * @param <T> the items type
     * @return a new subscriber
     */
    public static <T> AssertSubscriber<T> create(long requested) {
        return new AssertSubscriber<>(requested);
    }

    /**
     * Creates a new {@link AssertSubscriber} with 0 requested items and no upfront cancellation.
     *
     * @param context the context
     * @param <T> the items type
     * @return a new subscriber
     */
    public static <T> AssertSubscriber<T> create(Context context) {
        return new AssertSubscriber<>(context, 0, false);
    }

    /**
     * Creates a new {@link AssertSubscriber} with no upfront cancellation.
     *
     * @param context the context
     * @param requested the number of initially requested items
     * @param <T> the items type
     * @return a new subscriber
     */
    public static <T> AssertSubscriber<T> create(Context context, long requested) {
        return new AssertSubscriber<>(context, requested, false);
    }

    @Override
    public Context context() {
        return context;
    }

    /**
     * Assert that the multi has completed.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertCompleted() {
        shouldHaveCompleted(hasCompleted(), getFailure(), getItems());
        return this;
    }

    /**
     * Assert that the multi has failed.
     *
     * @param expectedTypeOfFailure the expected failure type
     * @param expectedFailureMessage a message to be contained in the failure message, or {@code null} when any message is fine
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertFailedWith(Class<? extends Throwable> expectedTypeOfFailure,
            String expectedFailureMessage) {
        shouldHaveFailed(hasCompleted(), getFailure(), expectedTypeOfFailure, expectedFailureMessage);
        return this;
    }

    /**
     * Assert that the multi has failed.
     *
     * @param expectedTypeOfFailure the expected failure type
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertFailedWith(Class<? extends Throwable> expectedTypeOfFailure) {
        shouldHaveFailed(hasCompleted(), getFailure(), expectedTypeOfFailure, null);
        return this;
    }

    /**
     * Assert that no item has been received yet.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertHasNotReceivedAnyItem() {
        shouldHaveReceivedNoItems(items);
        return this;
    }

    /**
     * Assert that the multi has been subscribed.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertSubscribed() {
        shouldBeSubscribed(numberOfSubscription);
        return this;
    }

    /**
     * Assert that the multi has not been subscribed.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertNotSubscribed() {
        shouldNotBeSubscribed(numberOfSubscription);
        return this;
    }

    /**
     * Assert that the multi has been terminated, i.e. received a failure or a completion event.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertTerminated() {
        shouldBeTerminated(hasCompleted(), getFailure());
        return this;
    }

    /**
     * Assert that the multi has not been terminated, i.e. did not received a failure or a completion event.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertNotTerminated() {
        shouldNotBeTerminated(hasCompleted(), getFailure());
        return this;
    }

    /**
     * Assert that a sequence of items has been received (in whole and in exact order).
     *
     * @param expected a sequence of items
     * @return this {@link AssertSubscriber}
     */
    @SafeVarargs
    public final AssertSubscriber<T> assertItems(T... expected) {
        shouldHaveReceivedExactly(items, expected);
        return this;
    }

    /**
     * @return get the last received item, potentially {@code null} if no items have been received.
     */
    public T getLastItem() {
        if (items.isEmpty()) {
            return null;
        } else {
            return items.get(items.size() - 1);
        }
    }

    /**
     * Asserts that the last received item is equal to {@code expected}.
     * The assertion fails if no items have been received.
     *
     * @param expected the expected item, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertLastItem(T expected) {
        shouldHaveReceived(getLastItem(), expected);
        return this;
    }

    /**
     * Awaits for the next item.
     * If no item have been received before the default timeout, an {@link AssertionError} is thrown.
     * <p>
     * Note that it requests one item from the upstream.
     *
     * @return this {@link AssertSubscriber}
     * @see #awaitNextItems(int, int)
     */
    public AssertSubscriber<T> awaitNextItem() {
        return awaitNextItems(1);
    }

    /**
     * Awaits for the next item.
     * If no item have been received before the given timeout, an {@link AssertionError} is thrown.
     * <p>
     * Note that it requests one item from the upstream.
     *
     * @param duration the timeout, must not be {@code null}
     * @return this {@link AssertSubscriber}
     * @see #awaitNextItems(int, int)
     */
    public AssertSubscriber<T> awaitNextItem(Duration duration) {
        return awaitNextItems(1, 1, duration);
    }

    /**
     * Awaits for the next {@code number} items.
     * If not enough items have been received before the default timeout, an {@link AssertionError} is thrown.
     * <p>
     * This method requests {@code number} items the upstream.
     *
     * @param number the number of items to expect, must be neither 0 nor negative.
     * @return this {@link AssertSubscriber}
     * @see #awaitNextItems(int, int)
     */
    public AssertSubscriber<T> awaitNextItems(int number) {
        return awaitNextItems(number, DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for the next {@code number} items.
     * If not enough items have been received before the default timeout, an {@link AssertionError} is thrown.
     *
     * @param number the number of items to expect, must neither be 0 nor negative.
     * @param request if not 0, the number of items to request upstream.
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitNextItems(int number, int request) {
        return awaitNextItems(number, request, DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for the next {@code number} items.
     * If not enough items have been received before the given timeout, an {@link AssertionError} is thrown.
     * <p>
     * This method requests {@code number} items upstream.
     *
     * @param number the number of items to expect, must be neither 0 nor negative.
     * @param duration the timeout, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitNextItems(int number, Duration duration) {
        return awaitNextItems(number, number, duration);
    }

    /**
     * Awaits for the next {@code number} items.
     * If not enough items have been received before the given timeout, an {@link AssertionError} is thrown.
     *
     * @param number the number of items to expect, must be neither 0 nor negative.
     * @param request if not 0, the number of items to request upstream.
     * @param duration the timeout, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitNextItems(int number, int request, Duration duration) {
        if (hasCompleted() || getFailure() != null) {
            if (hasCompleted()) {
                throw new AssertionError("Expecting a next items, but a completion event has already being received");
            } else {
                throw new AssertionError(
                        "Expecting a next items, but a failure event has already being received: " + getFailure());
            }
        }

        awaitNextItemEvents(number, request, duration);

        return this;
    }

    /**
     * Awaits for the subscriber to receive {@code number} items in total (including the ones received after calling
     * this method).
     * If not enough items have been received before the default timeout, an {@link AssertionError} is thrown.
     * <p>
     * Unlike {@link #awaitNextItems(int, int)}, this method does not request items from the upstream.
     *
     * @param number the number of items to expect, must be neither 0 nor negative.
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitItems(int number) {
        return awaitItems(number, DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for the subscriber to receive {@code number} items in total (including the ones received after calling
     * this method).
     * If not enough items have been received before the given timeout, an {@link AssertionError} is thrown.
     * <p>
     * Unlike {@link #awaitNextItems(int, int)}, this method does not requests items from the upstream.
     *
     * @param number the number of items to expect, must be neither 0 nor negative.
     * @param duration the timeout, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitItems(int number, Duration duration) {
        if (items.size() > number) {
            throw new AssertionError(
                    "Expected the number of items to be " + number + ", but it's already " + items.size());
        }

        if (isCancelled() || hasCompleted() || getFailure() != null) {
            if (items.size() != number) {
                throw new AssertionError(
                        "Expected the number of items to be " + number + ", but received " + items.size()
                                + " and we received a terminal event already");
            }
            return this;
        }

        awaitItemEvents(number, duration);

        return this;
    }

    /**
     * Awaits for a completion event.
     * It waits at most {@link #DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if a failure event is received instead of the expected completion, the check fails.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitCompletion() {
        return awaitCompletion(DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a completion event at most {@code duration}.
     * <p>
     * If the timeout expired, or if a failure event is received instead of the expected completion, the check fails.
     *
     * @param duration the duration, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitCompletion(Duration duration) {
        try {
            awaitEvent(terminal, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "No completion (or failure) event received in the last " + duration.toMillis() + " ms");
        }

        if (state == State.COMPLETED) {
            return this;
        }

        if (failure != null) {
            throw new AssertionError("Expected a completion event but got a failure: " + failure);
        }

        // We have been interrupted.
        return this;

    }

    /**
     * Awaits for a failure event.
     * It waits at most {@link #DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if a completion event is received instead of the expected failure, the check fails.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitFailure() {
        return awaitFailure(t -> {
        });
    }

    /**
     * Awaits for a failure event and validate it.
     * It waits at most {@link #DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, or if a completion event is received instead of the expected failure, the check fails.
     * The received failure is validated using the {@code assertion} consumer. The code of the consumer is expected to
     * throw an {@link AssertionError} to indicate that the failure didn't pass the validation. The consumer is not
     * called if no failures are received.
     *
     * @param assertion a check validating the received failure (if any). Must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitFailure(Consumer<Throwable> assertion) {
        return awaitFailure(assertion, DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a failure event.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, or if a completion event is received instead of the expected failure, the check fails.
     *
     * @param duration the max duration to wait, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitFailure(Duration duration) {
        return awaitFailure(t -> {
        }, duration);
    }

    /**
     * Awaits for a failure event and validate it.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, or if a completion event is received instead of the expected failure, the check fails.
     * The received failure is validated using the {@code assertion} consumer. The code of the consumer is expected to
     * throw an {@link AssertionError} to indicate that the failure didn't pass the validation. The consumer is not
     * called if no failures are received.
     *
     * @param assertion a check validating the received failure (if any). Must not be {@code null}
     * @param duration the max duration to wait, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitFailure(Consumer<Throwable> assertion, Duration duration) {
        try {
            awaitEvent(terminal, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "No completion (or failure) event received in the last " + duration.toMillis() + " ms");
        }

        if (state == State.COMPLETED) {
            throw new AssertionError("Expected a failure event but got a completion event.");
        }

        try {
            assertion.accept(failure);
            return this;
        } catch (AssertionError e) {
            throw new AssertionError("Received a failure event, but that failure did not pass the validation: " + e, e);
        }

    }

    /**
     * Awaits for a subscription event (the subscriber receives a {@link Flow.Subscription} from the upstream.
     * It waits at most {@link #DEFAULT_TIMEOUT}.
     * <p>
     * If the timeout expired, the check fails.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitSubscription() {
        return awaitSubscription(DEFAULT_TIMEOUT);
    }

    /**
     * Awaits for a subscription event (the subscriber receives a {@link Flow.Subscription} from the upstream.
     * It waits at most {@code duration}.
     * <p>
     * If the timeout expired, the check fails.
     *
     * @param duration the duration, must not be {@code null}
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> awaitSubscription(Duration duration) {
        try {
            awaitEvent(subscribed, duration);
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "Expecting a subscription event in the last " + duration.toMillis() + " ms, but did not get it");
        }
        return this;
    }

    private void awaitEvent(CountDownLatch latch, Duration duration) throws TimeoutException {
        // Are we already done?
        if (latch.getCount() == 0) {
            return;
        }
        try {
            if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new TimeoutException();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private final List<EventListener> eventListeners = new CopyOnWriteArrayList<>();

    private void awaitNextItemEvents(int number, int request, Duration duration) {
        NextItemTask<T> task = new NextItemTask<>(number, this);
        CompletableFuture<Void> future = task.future();
        if (request > 0) {
            request(request);
        }
        int size = items.size();
        try {
            future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            // Terminal event received
            int received = items.size() - size;
            if (isCancelled()) {
                throw new AssertionError(
                        "Expected " + number + " items, but received a cancellation event while waiting. Only "
                                + received
                                + " item(s) have been received.");
            } else if (hasCompleted()) {
                throw new AssertionError(
                        "Expected " + number + " items, but received a completion event while waiting. Only " + received
                                + " item(s) have been received.");
            } else {
                throw new AssertionError(
                        "Expected " + number + " items, but received a failure event while waiting: " + getFailure()
                                + ". Only "
                                + received + " item(s) have been received.");
            }
        } catch (TimeoutException e) {
            // Timeout
            int received = items.size() - size;
            throw new AssertionError(
                    "Expected " + number + " items in " + duration.toMillis() + " ms, but only received " + received
                            + " items.");
        }
    }

    private void awaitItemEvents(int expected, Duration duration) {
        ItemTask<T> task = new ItemTask<>(expected, duration.toMillis(), this);
        try {
            task.future().get(duration.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            if (expected == items.size()) {
                return;
            }
            // Terminal event received
            if (isCancelled()) {
                throw new AssertionError(
                        "Expected " + expected + " items, but received a cancellation event while waiting. Only "
                                + items.size()
                                + " items have been received.");
            } else if (hasCompleted()) {
                throw new AssertionError(
                        "Expected " + expected + " items, but received a completion event while waiting. Only " + items
                                .size()
                                + " items have been received.");
            } else if (getFailure() != null) {
                throw new AssertionError(
                        "Expected " + expected + " items, but received a failure event while waiting: " + getFailure()
                                + ". Only " + items.size() + " items have been received.");
            } else {
                e.printStackTrace();
                throw new AssertionError(
                        "Expected " + expected + " items.  Only " + items.size() + " items have been received.");
            }
        } catch (TimeoutException e) {
            // Timeout, but verify we didn't get event while timing out.
            if (items.size() >= expected) {
                return;
            }
            throw new AssertionError(
                    "Expected " + expected + " items.  Only " + items.size() + " items have been received.");
        }
    }

    /**
     * Cancel the subscription.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> cancel() {
        shouldBeSubscribed(numberOfSubscription);
        subscription.cancel();
        state = State.CANCELLED;
        Event ev = new Event(null, null, false, true);
        eventListeners.forEach(l -> l.accept(ev));
        return this;
    }

    /**
     * Request items.
     *
     * @param req the number of items to request.
     * @return this {@link AssertSubscriber}
     */
    public synchronized AssertSubscriber<T> request(long req) {
        Subscriptions.add(pendingRequests, req);
        if (state != State.INIT) {
            subscription.request(req);
        }
        return this;
    }

    @Override
    public synchronized void onSubscribe(Flow.Subscription s) {
        numberOfSubscription++;
        subscription = s;
        state = State.SUBSCRIBED;
        subscribed.countDown();
        if (upfrontCancellation) {
            s.cancel();
            state = State.CANCELLED;
            // Do not request if cancelled.
            return;
        }
        long pending = pendingRequests.get();
        if (pending > 0) {
            s.request(pending);
        }
    }

    @Override
    public synchronized void onItem(T t) {
        items.add(t);
        Event ev = new Event(t, null, false, false);
        eventListeners.forEach(l -> l.accept(ev));
        pendingRequests.decrementAndGet();
    }

    @Override
    public void onFailure(Throwable t) {
        state = State.FAILED;
        failure = t;
        terminal.countDown();
        Event ev = new Event(null, t, false, false);
        eventListeners.forEach(l -> l.accept(ev));
    }

    @Override
    public void onCompletion() {
        state = State.COMPLETED;
        terminal.countDown();
        Event ev = new Event(null, null, true, false);
        eventListeners.forEach(l -> l.accept(ev));
    }

    /**
     * The list of items that have been received.
     *
     * @return the list
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * The reported failure, if any.
     *
     * @return the failure or {@code null}
     */
    public Throwable getFailure() {
        return failure;
    }

    /**
     * Run an action.
     *
     * @param action the action
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> run(Runnable action) {
        try {
            action.run();
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
        return this;
    }

    /**
     * Check whether the subscription has been cancelled or not.
     *
     * @return a boolean
     */
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    /**
     * Check whether the multi has completed.
     *
     * @return a boolean
     */
    public boolean hasCompleted() {
        return state == State.COMPLETED;
    }

    private void registerListener(EventListener listener) {
        eventListeners.add(listener);
    }

    private void unregisterListener(EventListener listener) {
        eventListeners.remove(listener);
    }

    private interface EventListener extends Consumer<Event> {
    }

    private static class Event {

        private final Object item;
        private final Throwable failure;
        private final boolean completion;
        private final boolean cancellation;

        private Event(Object item, Throwable failure, boolean completion, boolean cancellation) {
            this.item = item;
            this.failure = failure;
            this.completion = completion;
            this.cancellation = cancellation;
        }

        public boolean isItem() {
            return item != null;
        }

        public boolean isCancellation() {
            return cancellation;
        }

        public boolean isFailure() {
            return failure != null;
        }

        public boolean isCompletion() {
            return completion;
        }
    }

    private static class NextItemTask<T> {

        private final int expected;
        private final AssertSubscriber<T> subscriber;

        public NextItemTask(int expected, AssertSubscriber<T> subscriber) {
            this.expected = expected;
            this.subscriber = subscriber;
        }

        public CompletableFuture<Void> future() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            AtomicInteger count = new AtomicInteger(this.expected);

            EventListener listener = event -> {
                if (event.isItem()) {
                    if (count.decrementAndGet() == 0) {
                        future.complete(null);
                    }
                } else if (event.isCancellation() || event.isFailure() || event.isCompletion()) {
                    future.completeExceptionally(
                            new NoSuchElementException("Received a terminal event while waiting for items"));
                }
                // Else wait for timeout or next event.
            };
            subscriber.registerListener(listener);
            return future
                    .whenComplete((x, f) -> subscriber.unregisterListener(listener));
        }
    }

    private static class ItemTask<T> {

        private final int expected;
        private final AssertSubscriber<T> subscriber;
        private final long duration;

        public ItemTask(int expected, long duration, AssertSubscriber<T> subscriber) {
            this.expected = expected;
            this.subscriber = subscriber;
            this.duration = duration;
        }

        public CompletableFuture<Void> future() {
            CompletableFuture<Void> future = new CompletableFuture<>();
            long timeout = System.currentTimeMillis() + duration;
            new Thread(() -> {
                while (System.currentTimeMillis() < timeout) {
                    if (subscriber.items.size() >= expected) {
                        future.complete(null);
                        return;
                    }
                    if (subscriber.isCancelled() || subscriber.getFailure() != null
                            || subscriber.hasCompleted()) {
                        future.completeExceptionally(
                                new NoSuchElementException("Received a terminal event while waiting for items"));
                        return;
                    }
                    try {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                future.completeExceptionally(new TimeoutException());
            }).start();
            return future;
        }
    }

}
