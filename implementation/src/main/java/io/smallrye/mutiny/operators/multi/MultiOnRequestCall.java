package io.smallrye.mutiny.operators.multi;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnRequestCall<T> extends AbstractMultiOperator<T, T> {

    private final LongFunction<Uni<?>> mapper;

    public MultiOnRequestCall(Multi<? extends T> upstream, LongFunction<Uni<?>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "consumer");
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.subscribe().withSubscriber(new MultiOnRequestCallOperator(nonNull(downstream, "downstream")));
    }

    class MultiOnRequestCallOperator extends MultiOperatorProcessor<T, T> {

        public MultiOnRequestCallOperator(MultiSubscriber<? super T> downstream) {
            super(downstream);
        }

        // Note that as per reactive streams specifications request and cancel calls need to happen serially
        private final AtomicReference<Cancellable> cancellable = new AtomicReference<>();

        /**
         * Amount of pending requests.
         */
        private final AtomicLong requests = new AtomicLong();

        @Override
        public void request(long numberOfItems) {
            ParameterValidation.positive(numberOfItems, "requests");
            // Check for potential overflow - expected by the TCK
            if (requests.addAndGet(numberOfItems) < 0) {
                throw new IllegalArgumentException("The amount of pending requests exceeded Long.MAX_VALUE");
            }
            cancellable.set(execute(numberOfItems).subscribe().with(
                    ignored -> {
                        cancellable.set(null);
                        super.request(numberOfItems);
                    },
                    throwable -> {
                        cancellable.set(null);
                        super.onFailure(throwable);
                    }));
        }

        @Override
        public void onItem(T item) {
            requests.decrementAndGet();
            super.onItem(item);
        }

        @Override
        public void cancel() {
            Cancellable uni = cancellable.getAndSet(null);
            if (uni != null) {
                uni.cancel();
            }
        }

        private Uni<?> execute(long numberOfItems) {
            try {
                return nonNull(mapper.apply(numberOfItems), "uni");
            } catch (Throwable err) {
                return Uni.createFrom().failure(err);
            }
        }
    }
}
