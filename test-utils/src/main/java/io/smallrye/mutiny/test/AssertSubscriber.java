package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

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

    public AssertSubscriber(long requested, boolean cancelled) {
        this(null, requested, cancelled);
    }

    public AssertSubscriber(Subscriber<T> spy, long requested, boolean cancelled) {
        this.requested.set(requested);
        this.upfrontCancellation = cancelled;
        this.spy = spy;
    }

    public AssertSubscriber(Subscriber<T> spy) {
        this(spy, 0, false);
    }

    public AssertSubscriber(long requested) {
        this(requested, false);
    }

    public static <T> AssertSubscriber<T> create() {
        return new AssertSubscriber<>(0);
    }

    public static <T> AssertSubscriber<T> create(long requested) {
        return new AssertSubscriber<>(requested);
    }

    public static <T> AssertSubscriber<T> create(Subscriber<T> spy) {
        return new AssertSubscriber<>(spy);
    }

    public AssertSubscriber<T> assertCompleted() {
        assertThat(completed).isTrue();
        assertThat(failure.get()).isNull();
        return this;
    }

    public AssertSubscriber<T> assertFailedWith(Class<? extends Throwable> typeOfException, String message) {
        if (failure.get() == null) {
            throw new AssertionError("The multi didn't failed");
        }
        if (completed.get()) {
            throw new AssertionError("The multi completed successfully");
        }

        Throwable throwable = failure.get();
        assertThat(throwable).isInstanceOf(typeOfException);
        if (message != null) {
            assertThat(throwable).hasMessageContaining(message);
        }

        return this;
    }

    public AssertSubscriber<T> assertHasNotReceivedAnyItem() {
        assertThat(items).isEmpty();
        return this;
    }

    public AssertSubscriber<T> assertSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(1);
        return this;
    }

    public AssertSubscriber<T> assertNotSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(0);
        return this;
    }

    public AssertSubscriber<T> assertTerminated() {
        assertThat(latch.getCount()).isEqualTo(0);
        return this;
    }

    public AssertSubscriber<T> assertNotTerminated() {
        assertThat(latch.getCount()).as("Multi did already complete").isGreaterThan(0);
        return this;
    }

    @SafeVarargs
    public final AssertSubscriber<T> assertItems(T... expected) {
        assertThat(items).containsExactly(expected);
        return this;
    }

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

    public AssertSubscriber<T> cancel() {
        assertThat(subscription.get()).as("No subscription").isNotNull();
        subscription.get().cancel();
        cancelled = true;
        return this;
    }

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

    public List<T> getItems() {
        return items;
    }

    public Throwable getFailure() {
        return failure.get();
    }

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

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean hasCompleted() {
        return completed.get();
    }
}
