package io.smallrye.mutiny.operators.multi.builders;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNullNpe;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.smallrye.mutiny.groups.GeneratorEmitter;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class GeneratorBasedMulti<T, S> extends AbstractMulti<T> {

    private final Supplier<S> initialStateSupplier;
    private final BiFunction<S, GeneratorEmitter<? super T>, S> generator;

    public GeneratorBasedMulti(Supplier<S> initialStateSupplier, BiFunction<S, GeneratorEmitter<? super T>, S> generator) {
        this.initialStateSupplier = initialStateSupplier;
        this.generator = generator;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        MultiSubscriber<? super T> downstream = nonNull(subscriber, "subscriber");
        S initialState;
        try {
            initialState = initialStateSupplier.get();
        } catch (Throwable err) {
            downstream.onFailure(err);
            return;
        }
        downstream.onSubscribe(new GeneratorSubscription(downstream, initialState));
    }

    class GeneratorSubscription implements Flow.Subscription, GeneratorEmitter<T> {

        private final MultiSubscriber<? super T> downstream;
        private S state;

        protected volatile boolean cancelled;
        protected final AtomicLong requested = new AtomicLong();

        GeneratorSubscription(MultiSubscriber<? super T> downstream, S state) {
            this.downstream = downstream;
            this.state = state;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0) {
                    if (n == Long.MAX_VALUE) {
                        generateAll();
                    } else {
                        generateSome(n);
                    }
                }
            } else {
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        private void doGenerate() {
            try {
                this.state = generator.apply(this.state, this);
            } catch (Throwable failure) {
                this.fail(failure);
            }
        }

        private void generateAll() {
            while (!cancelled) {
                doGenerate();
            }
        }

        private void generateSome(long n) {
            long emitted = 0L;
            long upperBound = n;

            for (;;) {
                if (cancelled) {
                    return;
                }

                while (!cancelled && emitted != upperBound) {
                    doGenerate();
                    emitted++;
                    if (cancelled) {
                        return;
                    }
                }

                if (emitted == upperBound) {
                    upperBound = requested.addAndGet(-emitted);
                    if (upperBound == 0L) {
                        return;
                    }
                    emitted = 0L;
                }
            }
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public void emit(T item) {
            if (!cancelled) {
                downstream.onItem(nonNullNpe(item, "item"));
            }
        }

        @Override
        public void fail(Throwable failure) {
            if (!cancelled) {
                cancelled = true;
                if (failure != null) {
                    downstream.onFailure(failure);
                } else {
                    downstream.onFailure(new NullPointerException("The failure is null"));
                }
            }
        }

        @Override
        public void complete() {
            if (!cancelled) {
                cancelled = true;
                downstream.onCompletion();
            }
        }
    }
}
