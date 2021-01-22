package io.smallrye.mutiny.operators.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.UniOperator;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import io.smallrye.mutiny.tuples.Functions;

public class UniOnTerminationCall<I> extends UniOperator<I, I> {

    private final Functions.Function3<? super I, Throwable, Boolean, Uni<?>> mapper;

    public UniOnTerminationCall(Uni<I> upstream,
            Functions.Function3<? super I, Throwable, Boolean, Uni<?>> mapper) {
        super(nonNull(upstream, "upstream"));
        this.mapper = nonNull(mapper, "mapper");
    }

    @Override
    protected void subscribing(UniSubscriber<? super I> subscriber) {
        upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<I, I>(subscriber) {
            private volatile Cancellable cancellable;

            /**
             * Guard that we call the mapper only once.
             */
            private final AtomicBoolean invoked = new AtomicBoolean();

            @Override
            public void onSubscribe(UniSubscription subscription) {
                subscriber.onSubscribe(new UniSubscription() {
                    @Override
                    public void cancel() {
                        // The downstream cancelled, we may be waiting for the produced uni to complete or fail.
                        // In this case, the mapper has already been called, we should not call it again.
                        if (cancellable != null) {
                            cancellable.cancel();
                            subscription.cancel();
                        } else {
                            // Cancellation happens while we haven't executed the mapper: invoke it and cancel.
                            execute(null, null, true).subscribe().with(
                                    ignored -> {
                                        subscription.cancel();
                                    },
                                    failure -> {
                                        Infrastructure.handleDroppedException(failure);
                                        subscription.cancel();
                                    });
                        }
                    }
                });
            }

            private Uni<?> execute(I item, Throwable failure, Boolean cancelled) {
                // Be sure the mapper is called only once.
                if (invoked.compareAndSet(false, true)) {
                    try {
                        return Objects.requireNonNull(mapper.apply(item, failure, cancelled), "Uni should not be null");
                    } catch (Throwable err) {
                        return Uni.createFrom().failure(err);
                    }
                } else {
                    return Uni.createFrom().nullItem();
                }
            }

            @Override
            public void onItem(I item) {
                cancellable = execute(item, null, false).subscribe().with(
                        ignored -> subscriber.onItem(item),
                        subscriber::onFailure);
            }

            @Override
            public void onFailure(Throwable failure) {
                cancellable = execute(null, failure, false).subscribe().with(
                        ignored -> subscriber.onFailure(failure),
                        ignored -> subscriber.onFailure(new CompositeException(failure, ignored)));
            }
        });
    }
}
