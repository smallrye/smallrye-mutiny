package io.smallrye.mutiny.operators.multi.builders;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class ResourceMulti<R, I> extends AbstractMulti<I> {
    private final Supplier<? extends R> resourceSupplier;
    private final Function<? super R, ? extends Publisher<I>> streamSupplier;
    private final Function<? super R, Uni<Void>> onCompletion;
    private final BiFunction<? super R, ? super Throwable, Uni<Void>> onFailure;
    private final Function<? super R, Uni<Void>> onCancellation;

    public ResourceMulti(
            Supplier<? extends R> resourceSupplier,
            Function<? super R, ? extends Publisher<I>> streamSupplier,
            Function<? super R, Uni<Void>> onCompletion,
            BiFunction<? super R, ? super Throwable, Uni<Void>> onFailure,
            Function<? super R, Uni<Void>> onCancellation) {
        this.resourceSupplier = resourceSupplier;
        this.streamSupplier = streamSupplier;
        this.onCompletion = onCompletion;
        this.onFailure = onFailure;
        this.onCancellation = onCancellation;
    }

    @Override
    public void subscribe(MultiSubscriber<? super I> subscriber) {
        R resource;
        try {
            resource = resourceSupplier.get();
            if (resource == null) {
                throw new IllegalArgumentException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        } catch (Throwable e) {
            Subscriptions.fail(subscriber, e);
            return;
        }

        Publisher<? extends I> stream;
        try {
            stream = streamSupplier.apply(resource);
            if (stream == null) {
                throw new IllegalArgumentException(ParameterValidation.SUPPLIER_PRODUCED_NULL);
            }
        } catch (Throwable e) {
            try {
                Uni<Void> uni = onFailure.apply(resource, e);
                if (uni == null) {
                    Subscriptions.fail(subscriber,
                            new NullPointerException("Unable to call the finalizer - it returned `null`"));
                } else {
                    uni.subscribe().with(
                            completed -> Subscriptions.fail(subscriber, e),
                            failed -> Subscriptions.fail(subscriber, new CompositeException(e, failed)));
                }
            } catch (Throwable ex) {
                Subscriptions.fail(subscriber, new CompositeException(e, ex));
                return;
            }
            Subscriptions.fail(subscriber, e);
            return;
        }

        ResourceSubscriber<I, R> us = new ResourceSubscriber<>(subscriber, resource, onCompletion, onFailure,
                onCancellation);
        stream.subscribe(us);
    }

    private static class ResourceSubscriber<I, R> implements Subscription, MultiSubscriber<I>, ContextSupport {

        private final MultiSubscriber<? super I> downstream;
        private final R resource;
        private final Function<? super R, Uni<Void>> onCompletion;
        private final BiFunction<? super R, ? super Throwable, Uni<Void>> onFailure;
        private final Function<? super R, Uni<Void>> onCancellation;
        private final AtomicBoolean terminated = new AtomicBoolean();
        private final AtomicReference<Subscription> upstream = new AtomicReference<>();

        public ResourceSubscriber(
                MultiSubscriber<? super I> downstream,
                R resource,
                Function<? super R, Uni<Void>> onCompletion,
                BiFunction<? super R, ? super Throwable, Uni<Void>> onFailure,
                Function<? super R, Uni<Void>> onCancellation) {
            this.downstream = downstream;
            this.resource = resource;
            this.onCompletion = onCompletion;
            this.onFailure = onFailure;
            this.onCancellation = onCancellation;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (upstream.compareAndSet(null, s)) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onItem(I item) {
            downstream.onNext(item);
        }

        @Override
        public void onFailure(Throwable failure) {
            Throwable innerError = null;
            Uni<Void> uni = null;
            if (terminated.compareAndSet(false, true)) {

                try {
                    uni = onFailure.apply(resource, failure);
                    if (uni == null) {
                        throw new NullPointerException("The finalizer produced a `null` Uni");
                    }
                } catch (Throwable e) {
                    innerError = e;
                }
            }

            Subscriptions.cancel(upstream);
            if (innerError != null) {
                downstream.onFailure(new CompositeException(failure, innerError));
            } else if (uni != null) {
                uni.subscribe().with(
                        completed -> downstream.onFailure(failure),
                        failed -> downstream.onFailure(new CompositeException(failure, failed)));
            }
        }

        @Override
        public void onCompletion() {
            Throwable innerError = null;
            Uni<Void> uni = null;
            if (terminated.compareAndSet(false, true)) {
                try {
                    uni = onCompletion.apply(resource);
                    if (uni == null) {
                        throw new NullPointerException("The finalizer produced a `null` Uni");
                    }
                } catch (Throwable e) {
                    innerError = e;
                }
            }

            Subscriptions.cancel(upstream);
            if (innerError != null) {
                downstream.onFailure(innerError);
            } else if (uni != null) {
                uni.subscribe().with(
                        completed -> downstream.onCompletion(),
                        downstream::onFailure);
            }
        }

        @Override
        public void request(long n) {
            upstream.get().request(n);
        }

        @Override
        public void cancel() {
            Throwable innerError = null;
            Uni<Void> uni = null;
            if (terminated.compareAndSet(false, true)) {
                try {
                    uni = onCancellation.apply(resource);
                    if (uni == null) {
                        throw new NullPointerException("The finalizer produced a `null` Uni");
                    }
                } catch (Throwable e) {
                    innerError = e;
                }
            }

            Subscriptions.cancel(upstream);
            if (innerError != null) {
                downstream.onFailure(innerError);
            } else if (uni != null) {
                uni.subscribe().with(
                        completed -> {
                        },
                        Infrastructure::handleDroppedException // ignore the failure, we have cancelled anyway.
                );
            }
        }

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }
    }
}
