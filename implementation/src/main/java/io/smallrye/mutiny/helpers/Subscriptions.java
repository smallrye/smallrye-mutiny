package io.smallrye.mutiny.helpers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.subscription.UniSubscription;

public class Subscriptions {

    public static final Throwable TERMINATED = new Exception("Terminated");

    private Subscriptions() {
        // avoid direct instantiation
    }

    public static IllegalArgumentException getInvalidRequestException() {
        return new IllegalArgumentException("Invalid request number, must be greater than 0");
    }

    public static Subscription empty() {
        return new EmptySubscription();
    }

    /**
     * This instance must not be shared.
     * Calling {@link Subscription#cancel()} is a no-op.
     */
    public static final EmptySubscription CANCELLED = new EmptySubscription();

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
     * @param failure the failure, must not be {@code null}
     */
    public static void fail(Subscriber<?> subscriber, Throwable failure) {
        fail(subscriber, failure, null);
    }

    public static void fail(Subscriber<?> subscriber, Throwable failure, Publisher<?> upstream) {
        ParameterValidation.nonNull(subscriber, "subscriber");
        ParameterValidation.nonNull(failure, "failure");
        if (upstream != null) {
            upstream.subscribe(new CancelledSubscriber<>());
        }

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
     * @param requests the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static long add(AtomicLong requested, long requests) {
        for (;;) {
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
     * @param emitted the produced element count, positive (not validated)
     * @return the new amount
     */
    public static long subtract(AtomicLong requested, long emitted) {
        for (;;) {
            long current = requested.get();
            if (current == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long update = current - emitted;
            if (update < 0L) {
                update = 0L;
            }
            if (requested.compareAndSet(current, update)) {
                return update;
            }
        }
    }

    public static int unboundedOrLimit(int prefetch) {
        return prefetch == Integer.MAX_VALUE ? Integer.MAX_VALUE : (prefetch - (prefetch >> 2));
    }

    public static long unboundedOrRequests(int concurrency) {
        return concurrency == Integer.MAX_VALUE ? Long.MAX_VALUE : concurrency;
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

    public static void terminateAndPropagate(AtomicReference<Throwable> failures, Subscriber<?> subscriber) {
        Throwable ex = markFailureAsTerminated(failures);
        if (ex == null) {
            subscriber.onComplete();
        } else if (ex != TERMINATED) {
            subscriber.onError(ex);
        }
    }

    /**
     * Cap a multiplication to Long.MAX_VALUE
     *
     * @param n left operand
     * @param times right operand
     * @return n * times or Long.MAX_VALUE
     */
    public static long multiply(long n, long times) {
        long u = n * times;
        if (((n | times) >>> 31) != 0) {
            if (u / n != times) {
                return Long.MAX_VALUE;
            }
        }
        return u;
    }

    /**
     * Atomically requests from the Subscription in the field if not null, otherwise accumulates
     * the request amount in the requested field to be requested once the field is set to non-null.
     *
     * @param field the target field that may already contain a Subscription
     * @param requested the current requested amount
     * @param requests the request amount, positive (verified)
     */
    public static void requestIfNotNullOrAccumulate(AtomicReference<Subscription> field, AtomicLong requested, long requests) {
        Subscription subscription = field.get();
        if (subscription != null) {
            subscription.request(requests);
        } else {
            if (requests > 0) {
                add(requested, requests);
                subscription = field.get();
                if (subscription != null) {
                    long r = requested.getAndSet(0L);
                    if (r != 0L) {
                        subscription.request(r);
                    }
                }
            }
        }
    }

    /**
     * Atomically sets the new {@link Subscription} in the container and requests any accumulated amount
     * from the requested counter.
     *
     * @param container the target field for the new Subscription
     * @param requested the current requested amount
     * @param subscription the new Subscription, must not be {@code null}
     * @return true if the Subscription was set the first time
     */
    public static boolean setIfEmptyAndRequest(AtomicReference<Subscription> container, AtomicLong requested,
            Subscription subscription) {
        if (Subscriptions.setIfEmpty(container, subscription)) {
            long r = requested.getAndSet(0L);
            if (r > 0L) {
                subscription.request(r);
            } else if (r < 0) {
                throw new IllegalArgumentException("Invalid amount of request");
            }
            return true;
        }
        return false;
    }

    /**
     * Atomically sets the subscription on the container if the content is still {@code null}.
     * If not the passed subscription gets cancelled.
     *
     * @param container the target container
     * @param subscription the new subscription to set
     * @return {@code true} if the operation succeeded, {@code false} if the target container was already set.
     */
    public static boolean setIfEmpty(AtomicReference<Subscription> container, Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription is null");
        if (!container.compareAndSet(null, subscription)) {
            subscription.cancel();
            return false;
        }
        return true;
    }

    public static Throwable terminate(AtomicReference<Throwable> failure) {
        return failure.getAndSet(TERMINATED);
    }

    public static class EmptySubscription implements Subscription, UniSubscription {

        @Override
        public void request(long requests) {
            ParameterValidation.positive(requests, "requests");
        }

        @Override
        public void cancel() {
            // Do nothing.
        }

    }

    /**
     * Concurrent subtraction bound to 0, mostly used to decrement a request tracker by
     * the amount produced by the operator.
     *
     * @param requested the atomic long keeping track of requests
     * @param amount delta to subtract
     * @return value after subtraction or zero
     */
    public static long produced(AtomicLong requested, long amount) {
        long r;
        long u;
        do {
            r = requested.get();
            if (r == 0 || r == Long.MAX_VALUE) {
                return r;
            }
            u = subOrZero(r, amount);
        } while (!requested.compareAndSet(r, u));

        return u;
    }

    /**
     * Cap a subtraction to 0
     *
     * @param a left operand
     * @param b right operand
     * @return Subtraction result or 0 if overflow
     */
    public static long subOrZero(long a, long b) {
        long res = a - b;
        if (res < 0L) {
            return 0;
        }
        return res;
    }

    public static <T> SingleItemSubscription<T> single(Subscriber<T> downstream, T item) {
        return new SingleItemSubscription<>(downstream, item);
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
        public void request(long requests) {
            if (requests > 0) {
                if (requested.compareAndSet(false, true)) {
                    downstream.onNext(item);
                    downstream.onComplete();
                }
            }
        }
    }

    @SuppressWarnings({ "ReactiveStreamsSubscriberImplementation" })
    public static class CancelledSubscriber<X> implements Subscriber<X> {
        @Override
        public void onSubscribe(Subscription s) {
            Objects.requireNonNull(s).cancel();
        }

        @Override
        public void onNext(X o) {
            // Ignored
        }

        @Override
        public void onError(Throwable t) {
            // Ignored
        }

        @Override
        public void onComplete() {
            // Ignored
        }
    }

    public static class DeferredSubscription implements Subscription {
        private final AtomicReference<Subscription> subscription = new AtomicReference<>();
        private final AtomicLong pendingRequests = new AtomicLong();

        protected boolean isCancelled() {
            return subscription.get() == CANCELLED;
        }

        @Override
        public void cancel() {
            Subscription actual = subscription.get();
            if (actual != CANCELLED) {
                actual = subscription.getAndSet(CANCELLED);
                if (actual != null && actual != CANCELLED) {
                    actual.cancel();
                }
            }
        }

        @Override
        public void request(long n) {
            Subscription actual = subscription.get();
            if (actual != null) {
                actual.request(n);
            } else {
                add(pendingRequests, n);
                actual = subscription.get();
                if (actual != null) {
                    long r = pendingRequests.getAndSet(0L);
                    if (r != 0L) {
                        actual.request(r);
                    }
                }
            }
        }

        /**
         * Atomically sets the single subscription and requests the missed amount from it.
         *
         * @param newSubscription the subscription to set
         * @return false if this arbiter is cancelled or there was a subscription already set
         */
        public boolean set(Subscription newSubscription) {
            ParameterValidation.nonNull(newSubscription, "newSubscription");
            Subscription actual = subscription.get();

            // Already cancelled.
            if (actual == CANCELLED) {
                newSubscription.cancel();
                return false;
            }

            // We already have a subscription, cancel the new one.
            if (actual != null) {
                newSubscription.cancel();
                return false;
            }

            if (subscription.compareAndSet(null, newSubscription)) {
                long r = pendingRequests.getAndSet(0L);
                if (r != 0L) {
                    newSubscription.request(r);
                }
                return true;
            }

            actual = this.subscription.get();

            if (actual != CANCELLED) {
                newSubscription.cancel();
                return false;
            }

            return false;
        }
    }

    /**
     * Atomically subtract the given number from the target atomic long if it doesn't contain {@link Long#MIN_VALUE}
     * (indicating some cancelled state) or {@link Long#MAX_VALUE} (unbounded mode).
     *
     * @param requested the target field holding the current requested amount
     * @param n the produced item count, must be positive
     * @return the new amount
     */
    public static long producedAndHandleAlreadyCancelled(AtomicLong requested, long n) {
        for (;;) {
            long current = requested.get();
            if (current == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
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
     * Atomically adds the positive value n to the requested value in the {@link AtomicLong} and
     * caps the result at {@link Long#MAX_VALUE} and returns the previous value and
     * considers {@link Long#MIN_VALUE} as a cancel indication (no addition then).
     *
     * @param requested the {@code AtomicLong} holding the current requested value
     * @param n the value to add, must be positive (not verified)
     * @return the original value before the add
     */
    public static long addAndHandledAlreadyCancelled(AtomicLong requested, long n) {
        for (;;) {
            long r = requested.get();
            if (r == Long.MIN_VALUE) {
                return Long.MIN_VALUE;
            }
            if (r == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            long u = add(r, n);
            if (requested.compareAndSet(r, u)) {
                return r;
            }
        }
    }
}
