package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.UniSubscriber;

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

    private class UniOnCancellationCallProcessor extends UniOperatorProcessor<I, I> {

        public UniOnCancellationCallProcessor(UniSubscriber<? super I> downstream) {
            super(downstream);
        }

        @Override
        public void cancel() {
            execute().subscribe().with(
                    ignoredItem -> super.cancel(),
                    ignoredException -> {
                        Infrastructure.handleDroppedException(ignoredException);
                        super.cancel();
                    });
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
