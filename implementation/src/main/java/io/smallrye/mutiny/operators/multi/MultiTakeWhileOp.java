package io.smallrye.mutiny.operators.multi;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.MultiSubscriber;

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
    public void subscribe(MultiSubscriber<? super T> actual) {
        ParameterValidation.nonNullNpe(actual, "subscriber");
        upstream.subscribe().withSubscriber(new TakeWhileProcessor<>(actual, predicate));
    }

    static final class TakeWhileProcessor<T> extends MultiOperatorProcessor<T, T> {
        private final Predicate<? super T> predicate;

        TakeWhileProcessor(MultiSubscriber<? super T> downstream, Predicate<? super T> predicate) {
            super(downstream);
            this.predicate = predicate;
        }

        @Override
        public void onItem(T t) {
            if (isDone()) {
                return;
            }

            boolean pass;
            try {
                pass = predicate.test(t);
            } catch (Throwable e) {
                failAndCancel(e);
                return;
            }

            if (!pass) {
                cancel();
                downstream.onCompletion();
                return;
            }

            downstream.onItem(t);
        }
    }
}
