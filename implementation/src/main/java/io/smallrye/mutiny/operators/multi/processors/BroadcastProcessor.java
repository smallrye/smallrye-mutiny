package io.smallrye.mutiny.operators.multi.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Implementation of {@link Processor} that broadcast all subsequently observed items to its current
 * {@link Subscriber}s.
 * <p>
 * This processor does not coordinate back-pressure between different subscribers and between the upstream source and a
 * subscriber. If an upstream item is received via {@link #onNext(Object)}, if a subscriber is not ready to receive that
 * item, that subscriber is terminated via a {@link io.smallrye.mutiny.subscription.BackPressureFailure}.
 * <p>
 * The {@code BroadcastProcessor}'s {@link Subscriber}-side consumes items in an unbounded manner.
 * <p>
 * When this {@code BroadcastProcessor} is terminated via {@link #onError(Throwable)} or {@link #onComplete()}, late
 * {@link Subscriber}s only receive the respective terminal event.
 * <p>
 * Unlike the {@link UnicastProcessor}, a {@code BroadcastProcessor} doesn't retain/cache items, therefore, a new
 * {@code Subscriber} won't receive any past items.
 * <p>
 * Even though {@code BroadcastProcessor} implements the {@link Subscriber} interface, calling {@code onSubscribe} is
 * not required if the processor is used as a <em>standalone</em> source. However, calling {@code onSubscribe} after
 * the {@code BroadcastProcessor} has failed or reached completion results in the given {@link Subscription} being
 * canceled immediately.
 */
public class BroadcastProcessor<T> extends AbstractMulti<T> implements Processor<T, T> {

    /**
     * Value indicating that the upstream has been cancelled.
     */
    static final List<?> TERMINATED = new ArrayList<>(0);

    /**
     * The array of currently subscribed subscribers.
     */
    final AtomicReference<List<BroadcastSubscription<T>>> subscribers;

    /**
     * The failure, write before terminating and read after checking subscribers.
     */
    Throwable failure;

    /**
     * Creates a new {@code BroadcastProcessor}
     *
     * @param <T> the type of item
     * @return the new {@code BroadcastProcessor}
     */
    public static <T> BroadcastProcessor<T> create() {
        return new BroadcastProcessor<>();
    }

    /**
     * Constructs a BroadcastProcessor.
     */
    private BroadcastProcessor() {
        subscribers = new AtomicReference<>(new CopyOnWriteArrayList<>());
    }

    public SerializedProcessor<T, T> serialized() {
        return new SerializedProcessor<>(this);
    }

    /**
     * Tries to add the given subscriber to the subscribers array atomically
     * or returns {@code false} if this processor has terminated.
     *
     * @param sub the subscriber to add
     * @return {@code true} if successful, {@code false} if this processor has terminated
     */
    private boolean addSubscription(BroadcastSubscription<T> sub) {
        List<BroadcastSubscription<T>> current = subscribers.get();
        if (current == TERMINATED) {
            return false;
        }
        return current.add(sub);
    }

    /**
     * Atomically removes the given subscriber if it is subscribed to this processor.
     *
     * @param sub the subscription wrapping a subscriber to remove
     */
    void remove(BroadcastSubscription<T> sub) {
        List<BroadcastSubscription<T>> current = subscribers.get();
        if (current == TERMINATED || current.isEmpty()) {
            return;
        }
        current.remove(sub);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        BroadcastSubscription<T> subscription = new BroadcastSubscription<>(downstream, this);
        downstream.onSubscribe(subscription);
        if (addSubscription(subscription)) {
            // if cancellation happened while a successful add, the remove() didn't work so we need to do it again
            if (subscription.isCancelled()) {
                remove(subscription);
            }
        } else {
            Throwable ex = failure;
            if (ex != null) {
                downstream.onFailure(ex);
            } else {
                downstream.onCompletion();
            }
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (subscribers.get() == TERMINATED) {
            subscription.cancel();
            return;
        }
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        ParameterValidation.nonNullNpe(item, "item");
        for (BroadcastSubscription<T> s : subscribers.get()) {
            s.onNext(item);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onError(Throwable failure) {
        ParameterValidation.nonNullNpe(failure, "failure");
        if (subscribers.get() == TERMINATED) {
            return;
        }
        this.failure = failure;
        List<BroadcastSubscription<?>> andSet = subscribers.getAndSet((List) TERMINATED);
        for (BroadcastSubscription<?> s : andSet) {
            s.onError(failure);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onComplete() {
        if (subscribers.get() == TERMINATED) {
            return;
        }
        List<BroadcastSubscription<?>> andSet = subscribers.getAndSet((List) TERMINATED);
        for (BroadcastSubscription<?> s : andSet) {
            s.onComplete();
        }
    }

    /**
     * Wraps the actual subscriber, tracks its requests and makes cancellation
     * to remove itself from the current subscribers array.
     *
     * @param <T> the type of item
     */
    static final class BroadcastSubscription<T> implements Subscription {

        /**
         * The actual subscriber.
         **/
        private final Subscriber<? super T> downstream;

        /**
         * The parent processor using this subscriber.
         */
        private final BroadcastProcessor<T> parent;

        /**
         * Pending requests.
         * {@code Long.MIN_VALUE} indicates cancellation.
         */
        private final AtomicLong requests = new AtomicLong();

        /**
         * Constructs a BroadcastSubscription, wraps the actual subscriber and the state.
         *
         * @param actual the actual subscriber
         * @param parent the parent PublishProcessor
         */
        BroadcastSubscription(Subscriber<? super T> actual, BroadcastProcessor<T> parent) {
            this.downstream = actual;
            this.parent = parent;
        }

        public void onNext(T t) {
            long r = requests.get();
            if (r == Long.MIN_VALUE) {
                return;
            }
            if (r != 0L) {
                downstream.onNext(t);
                Subscriptions.producedAndHandleAlreadyCancelled(requests, 1);
            } else {
                cancel();
                downstream.onError(new BackPressureFailure("Could not emit item downstream due to lack of requests"));
            }
        }

        public void onError(Throwable t) {
            if (requests.get() != Long.MIN_VALUE) {
                downstream.onError(t);
            }
        }

        public void onComplete() {
            if (requests.get() != Long.MIN_VALUE) {
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.addAndHandledAlreadyCancelled(requests, n);
            }
        }

        @Override
        public void cancel() {
            if (requests.getAndSet(Long.MIN_VALUE) != Long.MIN_VALUE) {
                parent.remove(this);
            }
        }

        public boolean isCancelled() {
            return requests.get() == Long.MIN_VALUE;
        }
    }
}
