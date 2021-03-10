package io.smallrye.mutiny.operators.multi;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiRepeatWhilstOp<T> extends AbstractMultiOperator<T, T> implements Multi<T> {
    private final Predicate<T> predicate;
    private final long times;
    private final Uni<?> delay;

    public MultiRepeatWhilstOp(Multi<T> upstream, Predicate<T> predicate, Uni<?> delay) {
        super(upstream);
        this.predicate = predicate;
        this.times = Long.MAX_VALUE;
        this.delay = delay;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "downstream");
        RepeatWhilstProcessor<T> processor = new RepeatWhilstProcessor<>(upstream, downstream,
                times != Long.MAX_VALUE ? times - 1 : Long.MAX_VALUE,
                predicate, delay);
        downstream.onSubscribe(processor);
        upstream.subscribe(processor);
    }

    static final class RepeatWhilstProcessor<T> extends MultiRepeatUntilOp.RepeatProcessor<T> {

        private boolean stop = false;

        public RepeatWhilstProcessor(Multi<? extends T> upstream, MultiSubscriber<? super T> downstream,
                long times, Predicate<T> predicate, Uni<?> delay) {
            super(upstream, downstream, times, predicate, delay);
        }

        @Override
        public void onItem(T t) {
            try {
                stop = !predicate.test(t);
            } catch (Throwable failure) {
                cancel();
                downstream.onError(failure);
                return;
            }
            emitted++;
            downstream.onNext(t);
        }

        @Override
        public void onCompletion() {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }

            if (r != 0L && !stop) {
                subscribeNext();
            } else {
                downstream.onComplete();
            }
        }
    }

}
