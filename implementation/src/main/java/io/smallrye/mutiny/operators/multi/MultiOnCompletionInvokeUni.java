package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnCompletionInvokeUni<T> extends AbstractMultiOperator<T, T> {

    private final Supplier<Uni<?>> supplier;

    public MultiOnCompletionInvokeUni(Multi<? extends T> upstream, Supplier<Uni<?>> supplier) {
        super(nonNull(upstream, "upstream"));
        this.supplier = nonNull(supplier, "supplier");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnCompletionInvokeUniProcessor(nonNull(downstream, "downstream")));
    }

    class MultiOnCompletionInvokeUniProcessor extends MultiOperatorProcessor<T, T> {

        private final AtomicBoolean supplierInvoked = new AtomicBoolean();
        private volatile Cancellable cancellable;

        public MultiOnCompletionInvokeUniProcessor(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onCompletion() {
            cancellable = execute().subscribe().with(
                    ignored -> super.onCompletion(),
                    err -> super.onFailure(err));
        }

        @Override
        public void cancel() {
            if (cancellable != null) {
                cancellable.cancel();
            }
            super.cancel();
        }

        private Uni<?> execute() {
            if (supplierInvoked.compareAndSet(false, true)) {
                try {
                    return nonNull(supplier.get(), "uni");
                } catch (Throwable err) {
                    return Uni.createFrom().failure(err);
                }
            } else {
                return Uni.createFrom().nullItem();
            }
        }
    }
}
