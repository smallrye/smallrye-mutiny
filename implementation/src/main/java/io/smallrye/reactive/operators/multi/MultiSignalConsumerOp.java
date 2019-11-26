package io.smallrye.reactive.operators.multi;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;

/**
 * Attach consumers to the various events and signals received by this {@link org.reactivestreams.Publisher}.
 * Consumer methods can be {@code null}
 *
 * @param <T> the value type
 */
public final class MultiSignalConsumerOp<T> extends AbstractMultiOperator<T, T> {

    private final Consumer<? super Subscription> onSubscribe;

    private final Consumer<? super T> onItem;

    private final Consumer<? super Throwable> onFailure;

    private final Runnable onCompletion;

    private final BiConsumer<Throwable, Boolean> onTermination;

    private final Runnable onCancellation;

    private final LongConsumer onRequest;

    public MultiSignalConsumerOp(Multi<? extends T> upstream,
            Consumer<? super Subscription> onSubscribe,
            Consumer<? super T> onItem,
            Consumer<? super Throwable> onFailure,
            Runnable onCompletion,
            BiConsumer<Throwable, Boolean> onTermination,
            LongConsumer onRequest,
            Runnable onCancellation) {
        super(upstream);
        this.onSubscribe = onSubscribe;
        this.onItem = onItem;
        this.onFailure = onFailure;
        this.onCompletion = onCompletion;
        this.onRequest = onRequest;
        this.onTermination = onTermination;
        this.onCancellation = onCancellation;
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new SignalSubscriber(actual));
    }

    @SuppressWarnings("SubscriberImplementation")
    private final class SignalSubscriber implements Subscriber<T>, Subscription {

        private final Subscriber<? super T> downstream;
        private final AtomicReference<Subscription> subscription = new AtomicReference<>();

        SignalSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            if (onRequest != null) {
                try {
                    onRequest.accept(n);
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
            }

            subscription.get().request(n);
        }

        @Override
        public void cancel() {
            if (onCancellation != null) {
                try {
                    onCancellation.run();
                } catch (Throwable e) {
                    onError(e);
                    return;
                }
            }
            if (onTermination != null) {
                try {
                    onTermination.accept(null, true);
                } catch (Throwable e) {
                    // Nothing we can do.
                    return;
                }
            }
            subscription.getAndSet(Subscriptions.CANCELLED).cancel();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription.compareAndSet(null, s)) {
                if (onSubscribe != null) {
                    try {
                        onSubscribe.accept(s);
                    } catch (Throwable e) {
                        Subscriptions.fail(downstream, e);
                        subscription.getAndSet(Subscriptions.CANCELLED).cancel();
                        return;
                    }
                }
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (subscription.get() != Subscriptions.CANCELLED) {
                if (onItem != null) {
                    try {
                        onItem.accept(t);
                    } catch (Throwable e) {
                        onError(e);
                        return;
                    }
                }
                downstream.onNext(t);
            }

        }

        @Override
        public void onError(Throwable failure) {
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onFailure != null) {
                    try {
                        onFailure.accept(failure);
                    } catch (Throwable e) {
                        failure = new CompositeException(failure, e);
                    }
                }

                downstream.onError(failure);

                if (onTermination != null) {
                    try {
                        onTermination.accept(failure, false);
                    } catch (Throwable e) {
                        // Nothing we can do...
                    }
                }
            }
        }

        @Override
        public void onComplete() {
            if (subscription.getAndSet(Subscriptions.CANCELLED) != Subscriptions.CANCELLED) {
                if (onCompletion != null) {
                    try {
                        onCompletion.run();
                    } catch (Throwable e) {
                        downstream.onError(e);
                        return;
                    }
                }

                if (onTermination != null) {
                    try {
                        onTermination.accept(null, false);
                    } catch (Throwable e) {
                        downstream.onError(e);
                        return;
                    }
                }

                downstream.onComplete();
            }
        }

    }

}
