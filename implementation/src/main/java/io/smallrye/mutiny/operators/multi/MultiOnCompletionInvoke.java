package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnCompletionInvoke<T> extends AbstractMultiOperator<T, T> {

    private final Runnable action;

    public MultiOnCompletionInvoke(Multi<? extends T> upstream, Runnable action) {
        super(upstream);
        this.action = action;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnCompletionInvokeProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnCompletionInvokeProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicBoolean actionInvoked = new AtomicBoolean();

        public MultiOnCompletionInvokeProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onCompletion() {
            if (actionInvoked.compareAndSet(false, true)) {
                try {
                    action.run();
                    super.onCompletion();
                } catch (Throwable err) {
                    super.onFailure(err);
                }
            }
        }
    }
}
