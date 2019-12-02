package io.smallrye.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

@SuppressWarnings("SubscriberImplementation")
public class MultiAssertSubscriber<T> implements Subscriber<T> {

    private final CountDownLatch latch = new CountDownLatch(1);

    private volatile Subscription subscription;

    private AtomicLong requested = new AtomicLong();

    private List<T> items = new CopyOnWriteArrayList<>();
    private List<Throwable> failures = new CopyOnWriteArrayList<>();

    private int numberOfSubscription = 0;

    private int numberOfCompletionEvents = 0;
    private boolean upfrontCancellation;

    public MultiAssertSubscriber(long requested, boolean cancelled) {
        this.requested.set(requested);
        upfrontCancellation = cancelled;
    }

    public MultiAssertSubscriber(long requested) {
        this(requested, false);
    }

    public static <T> MultiAssertSubscriber<T> create() {
        return new MultiAssertSubscriber<>(0);
    }

    public static <T> MultiAssertSubscriber<T> create(long requested) {
        return new MultiAssertSubscriber<>(requested);
    }

    public MultiAssertSubscriber<T> assertCompletedSuccessfully() {
        assertHasNotFailed();
        int num = numberOfCompletionEvents;
        if (num == 0) {
            throw new AssertionError("Not yet completed");
        }
        if (num > 1) {
            throw new AssertionError("Too many completions: " + num);
        }
        return this;
    }

    public MultiAssertSubscriber<T> assertHasFailedWith(Class<? extends Throwable> typeOfException, String message) {
        assertHasNotCompleted();
        int count = failures.size();
        if (count == 0) {
            throw new AssertionError("The multi didn't failed");
        }
        if (count > 1) {
            throw new AssertionError("The multi emitted several failure events errors: " + count);
        }

        Throwable throwable = failures.get(0);
        assertThat(throwable).isInstanceOf(typeOfException);
        if (message != null) {
            assertThat(throwable).hasMessageContaining(message);
        }

        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotFailed() {
        assertThat(failures).hasSize(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotReceivedAnyItem() {
        assertThat(items).isEmpty();
        return this;
    }

    public MultiAssertSubscriber<T> assertHasNotCompleted() {
        assertThat(numberOfCompletionEvents).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertNotSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertSubscribed() {
        assertThat(numberOfSubscription).isEqualTo(1);
        return this;
    }

    public MultiAssertSubscriber<T> assertTerminated() {
        assertThat(latch.getCount()).isEqualTo(0);
        return this;
    }

    public MultiAssertSubscriber<T> assertNotTerminated() {
        assertThat(latch.getCount()).as("Multi did not complete yet").isGreaterThan(0);
        return this;
    }

    @SafeVarargs
    public final MultiAssertSubscriber<T> assertReceived(T... expected) {
        assertThat(items).containsExactly(expected);
        return this;
    }

    public MultiAssertSubscriber<T> await() {
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

    public MultiAssertSubscriber<T> await(Duration duration) {
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

    public MultiAssertSubscriber<T> cancel() {
        assertThat(subscription).as("No subscription").isNotNull();
        subscription.cancel();
        return this;
    }

    public MultiAssertSubscriber<T> request(long req) {
        requested.addAndGet(req);
        if (subscription != null) {
            subscription.request(req);
        }
        return this;
    }

    @Override
    public void onSubscribe(Subscription s) {
        numberOfSubscription++;
        subscription = s;
        if (upfrontCancellation) {
            s.cancel();
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
        failures.add(t);
        latch.countDown();
    }

    @Override
    public void onComplete() {
        numberOfCompletionEvents++;
        latch.countDown();
    }

    public List<T> items() {
        return items;
    }

    public List<Throwable> failures() {
        return failures;
    }

    public MultiAssertSubscriber<T> run(Runnable action) {
        try {
            action.run();
        } catch (Throwable e) {
            if (e instanceof AssertionError) {
                throw e;
            } else {
                throw new AssertionError(e);
            }
        }
        return this;
    }
}
