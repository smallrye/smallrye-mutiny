package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniOnCancellationCall<I> extends UniOperator<I, I> {

    private final Supplier<Uni<?>> supplier;

    public UniOnCancellationCall(Uni<? extends I> upstream, Supplier<Uni<?>> supplier) {
        super(upstream);
        this.supplier = supplier;
    }

    @Override
    public void subscribe(UniSubscriber<? super I> subscriber) {
        AbstractUni.subscribe(upstream(), new UniOnCancellationCallProcessor(subscriber));
    }

    private enum State {
        INIT,
        DONE,
        CANCELLED
    }

    private class UniOnCancellationCallProcessor extends UniOperatorProcessor<I, I> {

        public UniOnCancellationCallProcessor(UniSubscriber<? super I> downstream) {
            super(downstream);
        }

        private final AtomicReference<State> state = new AtomicReference<>(State.INIT);

        @Override
        public void onItem(I item) {
            if (state.compareAndSet(State.INIT, State.DONE)) {
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (state.compareAndSet(State.INIT, State.DONE)) {
                downstream.onFailure(failure);
            }
        }

        @Override
        public void cancel() {
            if (state.compareAndSet(State.INIT, State.CANCELLED)) {
                UniSubscription sub = upstream.getAndSet(CANCELLED);
                execute().subscribe().with(
                        ignoredItem -> {
                            if (sub != null) {
                                sub.cancel();
                            }
                        },
                        ignoredException -> {
                            Infrastructure.handleDroppedException(ignoredException);
                            if (sub != null) {
                                sub.cancel();
                            }
                        });
            }
        }

        private Uni<?> execute() {
            try {
                return nonNull(supplier.get(), "uni");
            } catch (Throwable err) {
                return Uni.createFrom().failure(err);
            }
        }
    }
}
