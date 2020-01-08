package io.smallrye.mutiny.operators.multi.overflow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowKeepLastOp<T> extends AbstractMultiOperator<T, T> {

    public MultiOnOverflowKeepLastOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnOverflowLatestProcessor<T>(downstream));
    }

    static final class MultiOnOverflowLatestProcessor<T> extends MultiOperatorProcessor<T, T> {

        private final AtomicInteger wip = new AtomicInteger();
        private Throwable failure;
        private final AtomicLong requested = new AtomicLong();

        private volatile boolean done;
        private volatile boolean cancelled;

        private final AtomicReference<T> last = new AtomicReference<>();

        MultiOnOverflowLatestProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (upstream.compareAndSet(null, subscription)) {
                downstream.onSubscribe(this);
                subscription.request(Long.MAX_VALUE);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(T t) {
            last.lazySet(t);
            drain();
        }

        @Override
        public void onFailure(Throwable f) {
            failure = f;
            done = true;
            drain();
        }

        @Override
        public void onCompletion() {
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                Subscriptions.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                super.cancel();
                if (wip.getAndIncrement() == 0) {
                    last.lazySet(null);
                }
            }
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final AtomicLong req = requested;
            for (;;) {
                long emitted = 0L;

                while (emitted != req.get()) {
                    boolean isDone = done;
                    T v = last.getAndSet(null);
                    boolean isEmpty = v == null;

                    if (checkTerminated(isDone, isEmpty)) {
                        return;
                    }

                    if (isEmpty) {
                        break;
                    }

                    downstream.onItem(v);

                    emitted++;
                }

                if (emitted == req.get() && checkTerminated(done, last.get() == null)) {
                    return;
                }

                if (emitted != 0L) {
                    Subscriptions.subtract(req, emitted);
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminated(boolean wasDone, boolean wasEmpty) {
            if (cancelled) {
                last.lazySet(null);
                return true;
            }

            if (wasDone) {
                if (failure != null) {
                    last.lazySet(null);
                    super.onFailure(failure);
                    return true;
                } else if (wasEmpty) {
                    super.onCompletion();
                    return true;
                }
            }

            return false;
        }
    }
}
