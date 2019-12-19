package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public class MultiRepeatOp<T> extends AbstractMultiOperator<T, T> implements Multi<T> {
    private final long times;

    public MultiRepeatOp(Multi<T> upstream, long times) {
        super(upstream);
        this.times = times;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        RepeatProcessor<T> processor = new RepeatProcessor<>(upstream, downstream,
                times != Long.MAX_VALUE ? times - 1 : Long.MAX_VALUE);
        downstream.onSubscribe(processor);
        upstream.subscribe(processor);
    }

    static final class RepeatProcessor<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Multi<? extends T> upstream;
        private long remaining;
        private long emitted;
        private final AtomicInteger wip = new AtomicInteger();

        public RepeatProcessor(Multi<? extends T> upstream, MultiSubscriber<? super T> downstream, long times) {
            super(downstream);
            this.upstream = upstream;
            this.remaining = times;
        }

        @Override
        public void onSubscribe(Subscription s) {
            setOrSwitchUpstream(s);
        }

        @Override
        public void onItem(T t) {
            emitted++;
            downstream.onNext(t);
        }

        @Override
        public void onFailure(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onCompletion() {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }
            if (r != 0L) {
                subscribeNext();
            } else {
                downstream.onComplete();
            }
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (wip.getAndIncrement() == 0) {
                int missed = 1;
                while (!isCancelled()) {
                    long p = emitted;
                    if (p != 0L) {
                        emitted = 0L;
                        emitted(p);
                    }
                    upstream.subscribe(this);

                    missed = wip.addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }

}
