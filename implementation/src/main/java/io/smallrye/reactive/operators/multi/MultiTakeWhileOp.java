package io.smallrye.reactive.operators.multi;

import java.util.function.Predicate;

import org.reactivestreams.Subscriber;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;

/**
 * Emits the items from upstream while the given predicate returns {@code true} for the item.
 * The stream is completed once the predicate return {@code false}.
 *
 * @param <T> the type of item
 */
public final class MultiTakeWhileOp<T> extends AbstractMultiOperator<T, T> {

    private final Predicate<? super T> predicate;

    public MultiTakeWhileOp(Multi<? extends T> upstream, Predicate<? super T> predicate) {
        super(upstream);
        this.predicate = ParameterValidation.nonNull(predicate, "predicate");
    }

    @Override
    public void subscribe(Subscriber<? super T> actual) {
        upstream.subscribe(new TakeWhileProcessor<>(actual, predicate));
    }

    static final class TakeWhileProcessor<T> extends MultiOperatorProcessor<T, T> {
        private final Predicate<? super T> predicate;

        TakeWhileProcessor(Subscriber<? super T> downstream, Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onNext(T t) {
            if (isDone()) {
                return;
            }

            boolean pass;
            try {
                pass = predicate.test(t);
            } catch (Throwable e) {
                onError(e);
                return;
            }

            if (!pass) {
                cancel();
                downstream.onComplete();
                return;
            }

            downstream.onNext(t);
        }
    }
}
