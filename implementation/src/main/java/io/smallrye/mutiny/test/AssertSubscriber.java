package io.smallrye.mutiny.test;

import java.time.Duration;
import java.util.Arrays;
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
@SuppressWarnings({ "ReactiveStreamsSubscriberImplementation", "ConstantConditions" })
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
     * Whether the stream completed successfully.
     */
    private final AtomicBoolean completed = new AtomicBoolean();

    /**
     * A subscriber on which we delegate the events.
     * Useful to verify which method is called.
     */
    private final Subscriber<T> spy;

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
        this(null, requested, cancelled);
    }

    /**
     * Creates a new {@link AssertSubscriber}.
     * 
     * @param spy a subscriber to delegate the events to
     * @param requested the number of initially requested items
     * @param cancelled {@code true} if the subscription is immediately cancelled, {@code false} otherwise
     */
    public AssertSubscriber(Subscriber<T> spy, long requested, boolean cancelled) {
        this.requested.set(requested);
        this.upfrontCancellation = cancelled;
        this.spy = spy;
    }

    /**
     * Creates a new {@link AssertSubscriber} with 0 requested items and no upfront cancellation.
     *
     * @param spy a subscriber to delegate the events to
     */
    public AssertSubscriber(Subscriber<T> spy) {
        this(spy, 0, false);
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
     */
    public static <T> AssertSubscriber<T> create() {
        return new AssertSubscriber<>(0);
    }

    /**
     * Creates a new {@link AssertSubscriber} with no upfront cancellation.
     *
     * @param requested the number of initially requested items
     */
    public static <T> AssertSubscriber<T> create(long requested) {
        return new AssertSubscriber<>(requested);
    }

    /**
     * Creates a new {@link AssertSubscriber} from a delegate.
     *
     * @param spy a subscriber to delegate the events to
     */
    public static <T> AssertSubscriber<T> create(Subscriber<T> spy) {
        return new AssertSubscriber<>(spy);
    }

    /**
     * Assert that the stream has completed.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertCompleted() {
        if (!completed.get()) {
            throw new AssertionError("The stream has not completed");
        }
        if (failure.get() != null) {
            throw new AssertionError("The stream has not completed because of a failure", failure.get());
        }
        return this;
    }

    /**
     * Assert that the stream has failed.
     *
     * @param typeOfException the expected failure type
     * @param message a message to be contained in the failure message, or {@code null} when any message is fine
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertFailedWith(Class<? extends Throwable> typeOfException, String message) {
        if (failure.get() == null) {
            throw new AssertionError("The multi didn't failed");
        }
        if (completed.get()) {
            throw new AssertionError("The multi completed successfully");
        }

        Throwable throwable = failure.get();
        if (!(typeOfException.isInstance(failure.get()))) {
            throw new AssertionError("Expected the failure to be of type " + typeOfException.getCanonicalName() + " but was "
                    + failure.get().getClass().getCanonicalName());
        }
        if (message != null) {
            if (!throwable.getMessage().contains(message)) {
                throw new AssertionError("Expected the failure message to contain \"" + message + "\" but was: \""
                        + throwable.getMessage() + "\"");
            }
        }

        return this;
    }

    /**
     * Assert that no item has been received yet.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertHasNotReceivedAnyItem() {
        if (!items.isEmpty()) {
            throw new AssertionError("Items have been received");
        }
        return this;
    }

    /**
     * Assert that the stream has been subscribed.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertSubscribed() {
        if (numberOfSubscription != 1) {
            throw new AssertionError("Expected to be subscribed (number of subscriptions was " + numberOfSubscription + ")");
        }
        return this;
    }

    /**
     * Assert that the stream has not been subscribed.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertNotSubscribed() {
        if (numberOfSubscription != 0) {
            throw new AssertionError(
                    "Did not expect to be subscribed (number of subscriptions was " + numberOfSubscription + ")");
        }
        return this;
    }

    /**
     * Assert that the stream has been terminated.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertTerminated() {
        if (latch.getCount() != 0) {
            throw new AssertionError("Expected to be terminated");
        }
        return this;
    }

    /**
     * Assert that the stream has not been terminated.
     *
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> assertNotTerminated() {
        if (latch.getCount() == 0) {
            throw new AssertionError("Did not expect to be terminated");
        }
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
        if (items.size() != expected.length) {
            throw new AssertionError("Expected to have received: " + Arrays.toString(expected) + " but has received: " + items);
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(items.get(i))) {
                throw new AssertionError(
                        "Expected to have received: " + Arrays.toString(expected) + " but has received: " + items);
            }
        }
        return this;
    }

    /**
     * Await for the stream to be terminated.
     * 
     * @return this {@link AssertSubscriber}
     */
    public AssertSubscriber<T> await() {
        if (latch.getCount() == 0) {
            // We are done already.
            return this;
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * Await for the stream to be terminated.
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
                throw new AssertionError("Not terminated before timeout");
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
        if (subscription.get() == null) {
            throw new AssertionError("There is not subscription");
        }
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
        if (spy != null) {
            spy.onSubscribe(s);
        }

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
        if (spy != null) {
            spy.onNext(t);
        }
        items.add(t);
    }

    @Override
    public void onError(Throwable t) {
        if (spy != null) {
            spy.onError(t);
        }
        failure.set(t);
        latch.countDown();
    }

    @Override
    public void onComplete() {
        completed.set(true);
        if (spy != null) {
            spy.onComplete();
        }
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
     * Check whether the stream has completed.
     *
     * @return a boolean
     */
    public boolean hasCompleted() {
        return completed.get();
    }
}
