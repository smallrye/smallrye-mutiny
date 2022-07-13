package io.smallrye.mutiny.operators.multi.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow.Processor;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

/**
 * Wraps a processor into a serialized version of this processor.
 *
 * @param <I> the type of item from upstream
 * @param <O> the type of item propagated to the downstream
 */
public class SerializedProcessor<I, O> implements Processor<I, O> {
    /**
     * The actual subscriber to serialize Subscriber calls to.
     **/
    private final Processor<I, O> actual;

    /**
     * Indicates an emission is going on.
     * Access by be guarded by the monitor lock.
     **/
    boolean emitting;

    /**
     * If not null, it holds the missed notifications events.
     */
    private List<Object> queue;

    /**
     * Indicates a terminal event has been received and all further events will be dropped.
     **/
    volatile boolean done;

    /**
     * Constructor that wraps an actual processor.
     *
     * @param actual the subject wrapped
     */
    public SerializedProcessor(final Processor<I, O> actual) {
        this.actual = actual;
    }

    @Override
    public void subscribe(Subscriber<? super O> downstream) {
        actual.subscribe(downstream);
    }

    @Override
    public void onSubscribe(Subscription s) {
        boolean cancel;
        if (!done) {
            synchronized (this) {
                if (done) {
                    cancel = true;
                } else {
                    if (emitting) {
                        List<Object> q = getOrCreateQueue();
                        q.add(new SubscriptionEvent(s));
                        return;
                    }
                    emitting = true;
                    cancel = false;
                }
            }
        } else {
            cancel = true;
        }
        if (cancel) {
            s.cancel();
        } else {
            actual.onSubscribe(s);
            emitLoop();
        }
    }

    private List<Object> getOrCreateQueue() {
        List<Object> q = queue;
        if (q == null) {
            q = new ArrayList<>(4);
            queue = q;
        }
        return q;
    }

    @Override
    public void onNext(I item) {
        if (done) {
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                List<Object> q = getOrCreateQueue();
                q.add(new ItemEvent<>(item));
                return;
            }
            emitting = true;
        }
        actual.onNext(item);
        emitLoop();
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            } else {
                done = true;
                if (emitting) {
                    List<Object> q = getOrCreateQueue();
                    q.add(0, new FailureEvent(t));
                    return;
                }
                emitting = true;
            }
        }
        actual.onError(t);
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            done = true;
            if (emitting) {
                List<Object> q = getOrCreateQueue();
                q.add(new CompletionEvent());
                return;
            }
            emitting = true;
        }
        actual.onComplete();
    }

    /**
     * Loops until all notifications in the queue has been processed.
     */
    void emitLoop() {
        for (;;) {
            List<Object> q;
            synchronized (this) {
                q = queue;
                if (q == null) {
                    emitting = false;
                    return;
                }
                queue = null;
            }

            dispatch(q, actual);
        }
    }

    /**
     * Dispatches the events contained in the queue to the given subscriber.
     *
     * @param queue the queue of event
     * @param subscriber the subscriber to emit the events to
     */
    @SuppressWarnings("unchecked")
    public void dispatch(List<Object> queue, Subscriber<I> subscriber) {
        for (Object event : queue) {
            if (event != null) {
                if (event instanceof SerializedProcessor.SubscriptionEvent) {
                    subscriber.onSubscribe(((SubscriptionEvent) event).subscription);
                }

                if (event instanceof SerializedProcessor.FailureEvent) {
                    subscriber.onError(((FailureEvent) event).failure);
                    return;
                }

                if (event instanceof SerializedProcessor.CompletionEvent) {
                    subscriber.onComplete();
                    return;
                }

                if (event instanceof SerializedProcessor.ItemEvent) {
                    subscriber.onNext(((ItemEvent<I>) event).item);
                }
            }
        }
    }

    private static class SubscriptionEvent {
        private final Subscription subscription;

        private SubscriptionEvent(Subscription subscription) {
            this.subscription = subscription;
        }
    }

    private static class ItemEvent<T> {
        private final T item;

        private ItemEvent(T item) {
            this.item = item;
        }
    }

    private static class FailureEvent {
        private final Throwable failure;

        private FailureEvent(Throwable failure) {
            this.failure = failure;
        }
    }

    private static class CompletionEvent {
        private CompletionEvent() {
            // do nothing.
        }
    }
}
