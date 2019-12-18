package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public final class EmitterBasedMulti<T> extends AbstractMulti<T> {

    private final Consumer<MultiEmitter<? super T>> consumer;
    private final BackPressureStrategy backpressure;

    public EmitterBasedMulti(Consumer<MultiEmitter<? super T>> consumer, BackPressureStrategy backpressure) {
        this.consumer = consumer;
        this.backpressure = backpressure;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        BaseMultiEmitter<T> emitter;

        switch (backpressure) {
            case DROP:
                emitter = new DropItemOnOverflowMultiEmitter<>(downstream);
                break;

            case ERROR:
                emitter = new ErrorOnOverflowMultiEmitter<>(downstream);
                break;

            case IGNORE:
                emitter = new IgnoreBackPressureMultiEmitter<>(downstream);
                break;

            case LATEST:
                emitter = new DropLatestOnOverflowMultiEmitter<>(downstream);
                break;

            default:
                emitter = new BufferItemMultiEmitter<>(downstream, 16);
                break;

        }

        downstream.onSubscribe(emitter);
        try {
            consumer.accept(emitter.serialize());
        } catch (Throwable ex) {
            emitter.fail(ex);
        }
    }

    static final class IgnoreBackPressureMultiEmitter<T> extends BaseMultiEmitter<T> {

        IgnoreBackPressureMultiEmitter(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public MultiEmitter<T> emit(T item) {
            if (isCancelled()) {
                return this;
            }

            if (item != null) {
                downstream.onItem(item);
            } else {
                fail(new NullPointerException("`emit` called with `null`."));
                return this;
            }

            for (;;) {
                long r = requested.get();
                if (r == 0L || requested.compareAndSet(r, r - 1)) {
                    return this;
                }
            }
        }

    }

    abstract static class NoOverflowBaseMultiEmitter<T> extends BaseMultiEmitter<T> {

        NoOverflowBaseMultiEmitter(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public final MultiEmitter<T> emit(T t) {
            if (isCancelled()) {
                return this;
            }

            if (t == null) {
                fail(new NullPointerException(
                        "`emit` called with `null`."));
                return this;
            }

            if (requested.get() != 0) {
                downstream.onItem(t);
                Subscriptions.produced(requested, 1);
            } else {
                onOverflow();
            }
            return this;
        }

        abstract void onOverflow();
    }

    static final class DropItemOnOverflowMultiEmitter<T> extends NoOverflowBaseMultiEmitter<T> {

        DropItemOnOverflowMultiEmitter(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        void onOverflow() {
            // nothing to do, we drop the item.
        }

    }

    static final class ErrorOnOverflowMultiEmitter<T> extends NoOverflowBaseMultiEmitter<T> {

        ErrorOnOverflowMultiEmitter(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        void onOverflow() {
            fail(new BackPressureFailure("Could not emit value due to lack of requests"));
        }

    }

    static final class DropLatestOnOverflowMultiEmitter<T> extends BaseMultiEmitter<T> {

        private final AtomicReference<T> queue = new AtomicReference<>();
        private Throwable failure;
        private volatile boolean done;
        private final AtomicInteger wip = new AtomicInteger();

        DropLatestOnOverflowMultiEmitter(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public MultiEmitter<T> emit(T t) {
            if (done || isCancelled()) {
                return this;
            }

            if (t == null) {
                failed(new NullPointerException("onNext called with null."));
                return this;
            }
            queue.set(t);
            drain();
            return this;
        }

        @Override
        public void failed(Throwable e) {
            if (done || isCancelled()) {
                return;
            }
            if (e == null) {
                super.failed(new NullPointerException("onError called with null."));
            }
            failure = e;
            done = true;
            drain();
        }

        @Override
        public void completion() {
            done = true;
            drain();
        }

        @Override
        void onRequested() {
            drain();
        }

        @Override
        void onUnsubscribed() {
            if (wip.getAndIncrement() == 0) {
                queue.lazySet(null);
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final AtomicReference<T> q = queue;

            do {
                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (isCancelled()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    T o = q.getAndSet(null);

                    boolean empty = o == null;

                    if (d && empty) {
                        Throwable ex = failure;
                        if (ex != null) {
                            super.failed(ex);
                        } else {
                            super.completion();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    downstream.onItem(o);

                    e++;
                }

                if (e == r) {
                    if (isCancelled()) {
                        q.lazySet(null);
                        return;
                    }

                    boolean d = done;

                    boolean empty = q.get() == null;

                    if (d && empty) {
                        Throwable ex = failure;
                        if (ex != null) {
                            super.failed(ex);
                        } else {
                            super.completion();
                        }
                        return;
                    }
                }

                if (e != 0) {
                    Subscriptions.produced(requested, e);
                }

                missed = wip.addAndGet(-missed);
            } while (missed != 0);
        }
    }

}
