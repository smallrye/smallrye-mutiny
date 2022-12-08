package io.smallrye.mutiny.helpers;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Methods to implement <em>half-serialization</em>: a form of serialization where {@code onNext} is guaranteed to be
 * called from a single thread but {@code onError} or {@code onComplete} may be called from any threads.
 */
public final class HalfSerializer {
    private HalfSerializer() {
        // avoid direct instantiation.
    }

    /**
     * Propagates the given item if possible and terminates if there was a completion or failure event happening during
     * the propagation.
     * The item is drops is the downstream already got a terminal event.
     *
     * @param <T> the type of the item
     * @param subscriber the downstream subscriber
     * @param item the item to propagate downstream
     * @param wip the serialization work-in-progress counter
     * @param container the failure container
     */
    public static <T> void onNext(Subscriber<? super T> subscriber, T item,
            AtomicInteger wip, AtomicReference<Throwable> container) {
        if (wip.compareAndSet(0, 1)) {
            subscriber.onNext(item);
            if (wip.decrementAndGet() != 0) {
                Throwable ex = Subscriptions.terminate(container);
                if (ex != null) {
                    subscriber.onError(ex);
                } else {
                    subscriber.onComplete();
                }
            }
        } else {
            IllegalStateException err = new IllegalStateException(
                    "HalfSerializer has detected concurrent onNext(item) signals which is not permitted by the Reactive Streams protocol");
            onError(subscriber, err, wip, container);
        }
    }

    /**
     * Propagates the given failure if the downstream if possible (no work in progress) or accumulate it to the given
     * failure container to be propagated by a concurrent {@code onNext} call.
     *
     * @param subscriber the downstream subscriber
     * @param failure the failure event to propagate
     * @param wip the serialization work-in-progress counter
     * @param container the failure container
     */
    public static void onError(Subscriber<?> subscriber, Throwable failure,
            AtomicInteger wip, AtomicReference<Throwable> container) {
        if (Subscriptions.addFailure(container, failure)) {
            if (wip.getAndIncrement() == 0) {
                subscriber.onError(Subscriptions.terminate(container));
            }
        }
    }

    /**
     * Propagates the completion event or failure events (if a failure is stored in the container).
     * If the event cannot be dispatched, a concurrent {@code onNext} will.
     *
     * @param subscriber the downstream subscriber
     * @param wip the serialization work-in-progress counter
     * @param container the failure container
     */
    public static void onComplete(Subscriber<?> subscriber, AtomicInteger wip, AtomicReference<Throwable> container) {
        if (wip.getAndIncrement() == 0) {
            Throwable ex = Subscriptions.terminate(container);
            if (ex != null) {
                subscriber.onError(ex);
            } else {
                subscriber.onComplete();
            }
        }
    }

}
