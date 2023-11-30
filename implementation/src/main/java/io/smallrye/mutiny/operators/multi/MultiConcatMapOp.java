package io.smallrye.mutiny.operators.multi;

import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * ConcatMap operator without prefetching items from the upstream.
 * Requests are forwarded lazily to the upstream when:
 * <ul>
 * <li>First downstream request.</li>
 * <li>The inner has no more outstanding requests.</li>
 * <li>The inner completed without emitting items or with outstanding requests.</li>
 * </ul>
 * <p>
 * This operator can collect failures and postpone them until termination.
 *
 * @param <I> the upstream value type / input type
 * @param <O> the output value type / produced type
 */
public class MultiConcatMapOp<I, O> extends AbstractMultiOperator<I, O> {

    private final Function<? super I, ? extends Publisher<? extends O>> mapper;

    private final boolean postponeFailurePropagation;

    public MultiConcatMapOp(Multi<? extends I> upstream,
            Function<? super I, ? extends Publisher<? extends O>> mapper,
            boolean postponeFailurePropagation) {
        super(upstream);
        this.mapper = mapper;
        this.postponeFailurePropagation = postponeFailurePropagation;
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        if (subscriber == null) {
            throw new NullPointerException("The subscriber must not be `null`");
        }
        ConcatMapSubscriber<I, O> concatMapSubscriber = new ConcatMapSubscriber<>(mapper, postponeFailurePropagation,
                subscriber);
        upstream.subscribe(Infrastructure.onMultiSubscription(upstream, concatMapSubscriber));
    }

    static class ConcatMapSubscriber<I, O> implements MultiSubscriber<I>, Subscription, ContextSupport {

        private enum State {
            INIT,
            WAITING_NEXT_PUBLISHER,
            WAITING_NEXT_SUBSCRIPTION,
            EMITTING,
            CANCELLED,
        }

        private final Function<? super I, ? extends Publisher<? extends O>> mapper;
        private final boolean postponeFailurePropagation;
        private final MultiSubscriber<? super O> downstream;
        private volatile long demand = 0L;
        private volatile State state = State.INIT;
        private volatile Subscription upstream;
        private Subscription currentUpstream;
        private boolean upstreamHasCompleted = false;
        private Throwable failure;

        private static final AtomicReferenceFieldUpdater<ConcatMapSubscriber, Subscription> UPSTREAM_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(ConcatMapSubscriber.class, Subscription.class, "upstream");
        private static final AtomicReferenceFieldUpdater<ConcatMapSubscriber, State> STATE_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(ConcatMapSubscriber.class, State.class, "state");
        private static final AtomicLongFieldUpdater<ConcatMapSubscriber> DEMAND_UPDATER = AtomicLongFieldUpdater
                .newUpdater(ConcatMapSubscriber.class, "demand");

        ConcatMapSubscriber(Function<? super I, ? extends Publisher<? extends O>> mapper, boolean postponeFailurePropagation,
                MultiSubscriber<? super O> downstream) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.postponeFailurePropagation = postponeFailurePropagation;
        }

        @Override
        public Context context() {
            if (downstream instanceof ContextSupport) {
                return ((ContextSupport) downstream).context();
            } else {
                return Context.empty();
            }
        }

        private final MultiSubscriber<O> innerSubscriber = new InnerSubscriber();

        @Override
        public void onSubscribe(Subscription subscription) {
            if (UPSTREAM_UPDATER.compareAndSet(this, null, subscription)) {
                upstream = subscription;
                downstream.onSubscribe(this);
            } else {
                subscription.cancel();
            }
        }

        @Override
        public void onItem(I item) {
            if (state == State.CANCELLED) {
                return;
            }
            if (STATE_UPDATER.compareAndSet(this, State.WAITING_NEXT_PUBLISHER, State.WAITING_NEXT_SUBSCRIPTION)) {
                try {
                    Publisher<? extends O> publisher = mapper.apply(item);
                    if (publisher == null) {
                        throw new NullPointerException("The mapper produced a null publisher");
                    }
                    publisher.subscribe(innerSubscriber);
                } catch (Throwable err) {
                    upstream.cancel();
                    onFailure(err);
                }
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (STATE_UPDATER.getAndSet(this, State.CANCELLED) == State.CANCELLED) {
                return;
            }
            downstream.onFailure(addFailure(failure));
        }

        private Throwable addFailure(Throwable failure) {
            if (this.failure != null) {
                if (this.failure instanceof CompositeException) {
                    this.failure = new CompositeException((CompositeException) this.failure, failure);
                } else {
                    this.failure = new CompositeException(this.failure, failure);
                }
            } else {
                this.failure = failure;
            }
            return this.failure;
        }

        @Override
        public void onCompletion() {
            if (state == State.CANCELLED) {
                return;
            }
            upstreamHasCompleted = true;
            if (STATE_UPDATER.compareAndSet(this, State.WAITING_NEXT_PUBLISHER, State.CANCELLED)
                    || STATE_UPDATER.compareAndSet(this, State.INIT, State.CANCELLED)) {
                if (failure == null) {
                    downstream.onCompletion();
                } else {
                    downstream.onFailure(failure);
                }
            }
        }

        @Override
        public void request(long n) {
            if (state == State.CANCELLED) {
                return;
            }
            if (n <= 0) {
                cancel();
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            } else {
                Subscriptions.add(DEMAND_UPDATER, this, n);
                if (STATE_UPDATER.compareAndSet(this, State.INIT, State.WAITING_NEXT_PUBLISHER)) {
                    upstream.request(1L);
                } else {
                    if (state == State.WAITING_NEXT_PUBLISHER) {
                        upstream.request(1L);
                    } else if (state == State.EMITTING) {
                        currentUpstream.request(n);
                    }
                }
            }
        }

        @Override
        public void cancel() {
            State previousState = STATE_UPDATER.getAndSet(this, State.CANCELLED);
            if (previousState == State.CANCELLED) {
                return;
            }
            if (previousState == State.EMITTING) {
                currentUpstream.cancel();
                upstream.cancel();
            } else if (upstream != null) {
                upstream.cancel();
            }
        }

        class InnerSubscriber implements MultiSubscriber<O>, ContextSupport {

            @Override
            public void onSubscribe(Subscription subscription) {
                if (state == State.CANCELLED) {
                    return;
                }
                currentUpstream = subscription;
                state = State.EMITTING;
                long pending = demand;
                if (pending > 0L) {
                    currentUpstream.request(pending);
                }
            }

            @Override
            public void onItem(O item) {
                if (state == State.CANCELLED) {
                    return;
                }
                DEMAND_UPDATER.decrementAndGet(ConcatMapSubscriber.this);
                downstream.onItem(item);
            }

            @Override
            public void onFailure(Throwable failure) {
                if (state == State.CANCELLED) {
                    return;
                }
                state = State.WAITING_NEXT_PUBLISHER;
                Throwable err = addFailure(failure);
                if (postponeFailurePropagation) {
                    onCompletion();
                } else {
                    state = State.CANCELLED;
                    upstream.cancel();
                    downstream.onFailure(err);
                }
            }

            @Override
            public void onCompletion() {
                if (state == State.CANCELLED) {
                    return;
                }
                if (!upstreamHasCompleted) {
                    state = State.WAITING_NEXT_PUBLISHER;
                    if (demand > 0L) {
                        upstream.request(1L);
                    }
                } else {
                    state = State.CANCELLED;
                    if (failure != null) {
                        downstream.onFailure(failure);
                    } else {
                        downstream.onComplete();
                    }
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
}
