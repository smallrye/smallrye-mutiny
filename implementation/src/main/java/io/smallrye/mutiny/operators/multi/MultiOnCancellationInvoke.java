package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnCancellationInvoke<T> extends AbstractMultiOperator<T, T> {

    private final Runnable action;

    public MultiOnCancellationInvoke(Multi<? extends T> upstream, Runnable action) {
        super(nonNull(upstream, "upstream"));
        this.action = nonNull(action, "action");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnCancellationInvokeProcessor(downstream));
    }

    class MultiOnCancellationInvokeProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicBoolean actionInvoked = new AtomicBoolean();

        public MultiOnCancellationInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onCompletion() {
            actionInvoked.set(true);
            super.onCompletion();
        }

        @Override
        public void cancel() {
            if (actionInvoked.compareAndSet(false, true)) {
                try {
                    action.run();
                } catch (Throwable ignored) {
                    Infrastructure.handleDroppedException(ignored);
                }
            }
            super.cancel();
        }
    }
}
