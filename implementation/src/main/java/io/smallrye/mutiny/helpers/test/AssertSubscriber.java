package io.smallrye.mutiny.helpers.test;

import static io.smallrye.mutiny.helpers.test.AssertionHelper.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * A {@link io.smallrye.mutiny.Multi} {@link Subscriber} for testing purposes that comes with useful assertion helpers.
 * 
 * @param <T> the type of the items
 */
@SuppressWarnings({ "ReactiveStreamsSubscriberImplementation"})
public class AssertSubscriber<T> implements Subscriber<T> {

    /**
     * Latch waiting for the completion of failure event.
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * The subscription received from upstream.
     */
    private final AtomicReference<Subscription> subscription = new AtomicReference<>();

    /**
     * The number of requested items.
     */
    private final AtomicLong requested = new AtomicLong();

    /**
     * The received items.
     */
    private final List<T> items = new CopyOnWriteArrayList<>();

    /**
     * The received failure.
     */
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    /**
     * 1
     * Whether the multi completed successfully.
     */
    private final AtomicBoolean completed = new AtomicBoolean();

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
     * Whether the subscription has been cancelled.
     * This field is set to {@code true} when the subscriber calls {@code cancel} on the subscription.
     */
    private boolean cancelled;

    /**
     * Creates a new {@link AssertSubscriber}.
     *
     * @param requested the number of initially requested items
     * @param cancelled {@code true} if the subscription is immediately cancelled, {@code false} otherwise
     */
    public AssertSubscriber(long requested, boolean cancelled) {
        this.requested.set(requested);
        this.upfrontCancellation = cancelled;
    }

    /**
     * Creates a new {@link AssertSubscriber} with 0 requested items and no upfront cancellation.
     *
     */
    public AssertSubscriber() {
        this(0, false);
    }

    /**
     * Creates a new {@link AssertSubscriber} with no upfront cancellation.
     *
     * @param requested the number of initially requested items
     */
    public AssertSubscriber(long requested) {
        this(requested, false);
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
     * Assert that the multi has been terminated.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertTerminated() {
        shouldBeTerminated(hasCompleted(), getFailure());
        return this;
    }

    /**
     * Assert that the multi has not been terminated.
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
     * Await for the multi to be terminated.
     * Wait at most 10 seconds before failing.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> await() {
        return await(Duration.ofSeconds(10));
    }

    /**
     * Await for the multi to be terminated.
     *
     * @param duration the timeout duration
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> await(Duration duration) {
        if (latch.getCount() == 0) {
            // We are done already.
            return this;
        }

        try {
            if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Expected a terminal event before the timeout.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * Cancel the subscription.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> cancel() {
        shouldBeSubscribed(numberOfSubscription);
        subscription.get().cancel();
        cancelled = true;
        return this;
    }

    /**
     * Request items.
     * 
     * @param req the number of items to request.
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> request(long req) {
        requested.addAndGet(req);
        if (subscription.get() != null) {
            subscription.get().request(req);
        }
        return this;
    }

    @Override
    public void onSubscribe(Subscription s) {
        numberOfSubscription++;
        subscription.set(s);
        if (upfrontCancellation) {
            s.cancel();
            cancelled = true;
            // Do not request is cancelled.
            return;

        }
        if (requested.get() > 0) {
            s.request(requested.get());
        }

    }

    @Override
    public synchronized void onNext(T t) {
        items.add(t);
    }

    @Override
    public void onError(Throwable t) {
        failure.set(t);
        latch.countDown();
    }

    @Override
    public void onComplete() {
        completed.set(true);
        latch.countDown();
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
        return failure.get();
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
        return cancelled;
    }

    /**
     * Check whether the multi has completed.
     *
     * @return a boolean
     */
    public boolean hasCompleted() {
        return completed.get();
    }
}
