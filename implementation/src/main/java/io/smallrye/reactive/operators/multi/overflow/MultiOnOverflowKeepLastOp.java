package io.smallrye.reactive.operators.multi.overflow;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.Subscriptions;
import io.smallrye.reactive.operators.multi.AbstractMultiWithUpstream;
import io.smallrye.reactive.operators.multi.MultiOperatorSubscriber;

public class MultiOnOverflowKeepLastOp<T> extends AbstractMultiWithUpstream<T, T> {

    public MultiOnOverflowKeepLastOp(Multi<T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
        upstream.subscribe(new MultiOnOverflowLatestSubscriber<T>(downstream));
    }

    static final class MultiOnOverflowLatestSubscriber<T> extends MultiOperatorSubscriber<T, T> {

        private final AtomicInteger wip = new AtomicInteger();
        private Throwable failure;
        private final AtomicLong requested = new AtomicLong();

        private volatile boolean done;
        private volatile boolean cancelled;

        private final AtomicReference<T> last = new AtomicReference<>();

        MultiOnOverflowLatestSubscriber(Subscriber<? super T> downstream) {
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
        public void onNext(T t) {
            last.lazySet(t);
            drain();
        }

        @Override
        public void onError(Throwable f) {
            failure = f;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
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

                    downstream.onNext(v);

                    emitted++;
                }

                if (emitted == req.get() && checkTerminated(done, last.get() == null)) {
                    return;
                }

                if (emitted != 0L) {
                    Subscriptions.substract(req, emitted);
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
                    super.onError(failure);
                    return true;
                } else if (wasEmpty) {
                    super.onComplete();
                    return true;
                }
            }

            return false;
        }
    }
}
