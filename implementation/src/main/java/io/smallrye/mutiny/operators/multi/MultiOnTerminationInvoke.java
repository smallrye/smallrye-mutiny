package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnTerminationInvoke<T> extends AbstractMultiOperator<T, T> {

    private final BiConsumer<Throwable, Boolean> callback;

    public MultiOnTerminationInvoke(Multi<? extends T> upstream, BiConsumer<Throwable, Boolean> callback) {
        super(upstream);
        this.callback = callback;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnTerminationInvokeProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnTerminationInvokeProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicBoolean actionInvoke = new AtomicBoolean();

        public MultiOnTerminationInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onFailure(Throwable failure) {
            try {
                execute(failure, false);
                super.onFailure(failure);
            } catch (Throwable err) {
                super.onFailure(new CompositeException(failure, err));
            }
        }

        @Override
        public void onItem(T item) {
            downstream.onItem(item);
        }

        @Override
        public void onCompletion() {
            try {
                execute(null, false);
                super.onCompletion();
            } catch (Throwable err) {
                super.onFailure(err);
            }
        }

        @Override
        public void cancel() {
            try {
                execute(null, true);
            } catch (Throwable ignored) {
                Infrastructure.handleDroppedException(ignored);
            }
            super.cancel();
        }

        private void execute(Throwable err, Boolean cancelled) {
            if (actionInvoke.compareAndSet(false, true)) {
                callback.accept(err, cancelled);
            }
        }
    }
}
