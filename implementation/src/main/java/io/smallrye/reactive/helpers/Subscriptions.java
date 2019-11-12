package io.smallrye.reactive.helpers;

import io.smallrye.reactive.CompositeException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Subscriptions {

    public static final Throwable TERMINATED = new Exception("Terminated");

    private Subscriptions() {
        // avoid direct instantiation
    }

    /**
     * Check Subscription current state and cancel new Subscription if current is set,
     * or return true if ready to subscribe.
     *
     * @param current current Subscription, expected to be null
     * @param next    new Subscription
     * @return true if Subscription can be used
     */
    public static boolean validate(Subscription current, Subscription next) {
        Objects.requireNonNull(next, "Subscription cannot be null");
        if (current != null) {
            next.cancel();
            return false;
        }

        return true;
    }

    public static Subscription empty() {
        return new EmptySubscription();
    }

    /**
     * This instance must not be shared.
     * Calling {@link Subscription#cancel()} is a no-op.
     */
    public static final EmptySubscription CANCELLED = new EmptySubscription();

    public static <T> void propagateFailureEvent(Subscriber<T> subscriber, Throwable failure) {
        subscriber.onSubscribe(CANCELLED);
        if (failure == null) {
            subscriber.onError(new NullPointerException());
        } else {
            subscriber.onError(failure);
        }
    }

    /**
     * Invokes {@code onSubscribe} on the given {@link Subscriber} with the <em>cancelled</em> subscription instance
     * followed immediately by a call to {@code onComplete}.
     *
     * @param subscriber the subscriber, must not be {@code null}
     */
    public static void complete(Subscriber<?> subscriber) {
        ParameterValidation.nonNull(subscriber, "subscriber");
        subscriber.onSubscribe(empty());
        subscriber.onComplete();
    }

    /**
     * Invokes {@code onSubscribe} on the given {@link Subscriber} with the <em>cancelled</em> subscription instance
     * followed immediately by a call to {@code onError} with the given failure.
     *
     * @param subscriber the subscriber, must not be {@code null}
     * @param failure    the failure, must not be {@code null}
     */
    public static void fail(Subscriber<?> subscriber, Throwable failure) {
        ParameterValidation.nonNull(subscriber, "subscriber");
        ParameterValidation.nonNull(failure, "failure");
        subscriber.onSubscribe(empty());
        subscriber.onError(failure);
    }

    /**
     * Adds two long values and caps the sum at Long.MAX_VALUE.
     *
     * @param a the first value
     * @param b the second value
     * @return the sum capped at Long.MAX_VALUE
     */
    public static long add(long a, long b) {
        long u = a + b;
        if (u < 0L) {
            return Long.MAX_VALUE;
        }
        return u;
    }

    /**
     * Atomically adds the positive value n to the requested value in the AtomicLong and
     * caps the result at Long.MAX_VALUE and returns the previous value.
     *
     * @param requested the AtomicLong holding the current requested value
     * @param requests  the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static long add(AtomicLong requested, long requests) {
        for (; ; ) {
            long r = requested.get();
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = add(r, requests);
            if (requested.compareAndSet(r, u)) {
                return r;
            }
        }
    }

    /**
     * Atomically subtract the given number (positive, not validated) from the target field unless it contains Long.MAX_VALUE.
     *
     * @param requested the target field holding the current requested amount
     * @param n         the produced element count, positive (not validated)
     * @return the new amount
     */
    public static long substract(AtomicLong requested, long n) {
        for (; ; ) {
            long current = requested.get();
            if (current == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long update = current - n;
            if (update < 0L) {
                update = 0L;
            }
            if (requested.compareAndSet(current, update)) {
                return update;
            }
        }
    }

    /**
     * Evaluate if a request is strictly positive.
     *
     * @param n the request value
     * @return {@code true} if valid
     */
    public static boolean validate(long n) {
        return n > 0;
    }

    public static int unboundedOrLimit(int prefetch) {
        return prefetch == Integer.MAX_VALUE ? Integer.MAX_VALUE : (prefetch - (prefetch >> 2));
    }

    public static long unboundedOrMaxConcurrency(int concurrency) {
        return concurrency == Integer.MAX_VALUE ? Long.MAX_VALUE : concurrency;
    }

    public static int unboundedOrLimit(int prefetch, int lowTide) {
        if (lowTide <= 0) {
            return prefetch;
        }
        if (lowTide >= prefetch) {
            return unboundedOrLimit(prefetch);
        }
        return prefetch == Integer.MAX_VALUE ? Integer.MAX_VALUE : lowTide;
    }

    public static boolean addFailure(AtomicReference<Throwable> failures, Throwable failure) {
        Throwable current = failures.get();

        if (current == Subscriptions.TERMINATED) {
            return false;
        }

        if (current instanceof CompositeException) {
            failures.set(new CompositeException((CompositeException) current, failure));
            return true;
        }

        if (current == null) {
            failures.set(failure);
        } else {
            failures.set(new CompositeException(current, failure));
        }

        return true;
    }

    public static void cancel(AtomicReference<Subscription> reference) {
        Subscription actual = reference.getAndSet(CANCELLED);
        if (actual != null && actual != CANCELLED) {
            actual.cancel();
        }
    }

    public static Throwable markFailureAsTerminated(AtomicReference<Throwable> failures) {
        return failures.getAndSet(TERMINATED);
    }

    public static class EmptySubscription implements Subscription {

        @Override
        public void request(long requests) {
            ParameterValidation.positive(requests, "requests");
        }

        @Override
        public void cancel() {
            // Do nothing.
        }

    }

    public static <T> SingleItemSubscription<T> single(Subscriber<T> downstream, T item) {
        return new SingleItemSubscription<T>(downstream, item);
    }

    private static final class SingleItemSubscription<T> implements Subscription {

        private final Subscriber<? super T> downstream;

        private final T item;

        private AtomicBoolean requested = new AtomicBoolean();

        public SingleItemSubscription(Subscriber<? super T> actual, T item) {
            this.downstream = ParameterValidation.nonNull(actual, "actual");
            this.item = ParameterValidation.nonNull(item, "item");
        }

        @Override
        public void cancel() {
            // Make sure that another request won't emit the item.
            requested.lazySet(true);
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (requested.compareAndSet(false, true)) {
                    downstream.onNext(item);
                    downstream.onComplete();
                }
            }
        }
    }
}
