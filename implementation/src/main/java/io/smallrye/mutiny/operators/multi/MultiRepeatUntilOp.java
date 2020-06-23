package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.reactivestreams.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.SwitchableSubscriptionSubscriber;

public class MultiRepeatUntilOp<T> extends AbstractMultiOperator<T, T> implements Multi<T> {
    private final Predicate<T> predicate;
    private final long times;

    public MultiRepeatUntilOp(Multi<T> upstream, long times) {
        super(upstream);
        this.times = times;
        this.predicate = x -> false;
    }

    public MultiRepeatUntilOp(Multi<T> upstream, Predicate<T> predicate) {
        super(upstream);
        this.predicate = predicate;
        this.times = Long.MAX_VALUE;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        RepeatUntilProcessor<T> processor = new RepeatUntilProcessor<>(upstream, downstream,
                times != Long.MAX_VALUE ? times - 1 : Long.MAX_VALUE,
                predicate);
        downstream.onSubscribe(processor);
        upstream.subscribe(processor);
    }

    static final class RepeatUntilProcessor<T> extends SwitchableSubscriptionSubscriber<T> {

        private final Multi<? extends T> upstream;
        private long remaining;
        private final Predicate<T> predicate;

        /**
         * Stores the result of the last test of the predicate, i.e. the test with the last emitted item.
         * It indicates if the repetition must be stopped (the predicate would have returned {@code false}).
         * <p>
         * Access does not need to be synchronized as it is only accessed in onItem and onCompletion which cannot be
         * called concurrently.
         */
        private boolean passed;

        private long emitted;
        private final AtomicInteger wip = new AtomicInteger();

        public RepeatUntilProcessor(Multi<? extends T> upstream, MultiSubscriber<? super T> downstream,
                long times, Predicate<T> predicate) {
            super(downstream);
            this.upstream = upstream;
            this.predicate = predicate;
            this.remaining = times;
        }

        @Override
        public void onSubscribe(Subscription s) {
            setOrSwitchUpstream(s);
        }

        @Override
        public void onItem(T t) {
            passed = !predicate.test(t);
            if (passed) {
                emitted++;
                downstream.onNext(t);
            }
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
            if (r != 0L && passed) {
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
