package io.smallrye.mutiny.operators.multi;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.ReentrantLock;
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
        MainSubscriber<? super I, O> sub = new MainSubscriber<>(mapper, postponeFailurePropagation, subscriber);
        upstream.subscribe(Infrastructure.onMultiSubscription(upstream, sub));
    }

    private enum State {
        INIT,
        READY,
        PUBLISHER_REQUESTED,
        EMITTING,
        EMITTING_FINAL,
        DONE
    }

    private static class MainSubscriber<I, O> implements MultiSubscriber<I>, Flow.Subscription, ContextSupport {

        private final Function<? super I, ? extends Publisher<? extends O>> mapper;
        private final boolean postponeFailurePropagation;
        private final MultiSubscriber<? super O> downstream;

        private volatile State state = State.INIT;
        private static final AtomicReferenceFieldUpdater<MainSubscriber, State> STATE_UPDATER = AtomicReferenceFieldUpdater
                .newUpdater(MainSubscriber.class, State.class, "state");

        private volatile long demand = 0L;
        private static final AtomicLongFieldUpdater<MainSubscriber> DEMAND_UPDATER = AtomicLongFieldUpdater
                .newUpdater(MainSubscriber.class, "demand");

        private final InnerSubscriber innerSubscriber = new InnerSubscriber();
        private final ReentrantLock stateLock = new ReentrantLock();
        private volatile Throwable failure;
        private Flow.Subscription mainUpstream;
        private volatile Flow.Subscription innerUpstream;

        private MainSubscriber(Function<? super I, ? extends Publisher<? extends O>> mapper, boolean postponeFailurePropagation,
                MultiSubscriber<? super O> downstream) {
            this.mapper = mapper;
            this.postponeFailurePropagation = postponeFailurePropagation;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (STATE_UPDATER.compareAndSet(this, State.INIT, State.READY)) {
                mainUpstream = subscription;
                downstream.onSubscribe(this);
            } else {
                subscription.cancel();
            }
        }

        private void innerOnSubscribe(Flow.Subscription subscription) {
            stateLock.lock();
            innerUpstream = subscription;
            long n = demand;
            stateLock.unlock();
            if (n > 0L) {
                subscription.request(n);
            }
        }

        @Override
        public void onItem(I item) {
            if (STATE_UPDATER.compareAndSet(this, State.PUBLISHER_REQUESTED, State.EMITTING)) {
                try {
                    Publisher<? extends O> publisher = requireNonNull(mapper.apply(item),
                            "The mapper produced a null publisher");
                    publisher.subscribe(innerSubscriber);
                } catch (Throwable err) {
                    state = State.DONE;
                    mainUpstream.cancel();
                    downstream.onFailure(addFailure(err));
                }
            }
        }

        private void innerOnItem(O item) {
            if (state != State.DONE) {
                if (demand < Long.MAX_VALUE) {
                    DEMAND_UPDATER.decrementAndGet(this);
                }
                downstream.onItem(item);
            }
        }

        @Override
        public void onFailure(Throwable failure) {
            if (STATE_UPDATER.getAndSet(this, State.DONE) != State.DONE) {
                if (innerUpstream != null) {
                    innerUpstream.cancel();
                }
                downstream.onFailure(addFailure(failure));
            } else {
                Infrastructure.handleDroppedException(failure);
            }
        }

        private void innerOnFailure(Throwable failure) {
            Throwable throwable = addFailure(failure);
            stateLock.lock();
            switch (state) {
                case EMITTING:
                    if (postponeFailurePropagation) {
                        if (demand > 0L) {
                            state = State.PUBLISHER_REQUESTED;
                            stateLock.unlock();
                            mainUpstream.request(1L);
                        } else {
                            state = State.READY;
                            stateLock.unlock();
                        }
                    } else {
                        state = State.DONE;
                        stateLock.unlock();
                        mainUpstream.cancel();
                        downstream.onFailure(throwable);
                    }
                    break;
                case EMITTING_FINAL:
                    state = State.DONE;
                    stateLock.unlock();
                    mainUpstream.cancel();
                    downstream.onFailure(throwable);
                    break;
                default:
                    stateLock.unlock();
                    Infrastructure.handleDroppedException(failure);
                    break;
            }
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
            stateLock.lock();
            switch (state) {
                case EMITTING:
                    state = State.EMITTING_FINAL;
                    stateLock.unlock();
                    break;
                case READY:
                case PUBLISHER_REQUESTED:
                    stateLock.unlock();
                    terminate();
                    break;
                default:
                    stateLock.unlock();
                    break;
            }
        }

        private void innerOnCompletion() {
            stateLock.lock();
            switch (state) {
                case EMITTING:
                    if (demand > 0L) {
                        state = State.PUBLISHER_REQUESTED;
                        stateLock.unlock();
                        mainUpstream.request(1L);
                    } else {
                        state = State.READY;
                        stateLock.unlock();
                    }
                    break;
                case EMITTING_FINAL:
                    stateLock.unlock();
                    terminate();
                    break;
                default:
                    stateLock.unlock();
                    break;
            }
        }

        private void terminate() {
            if (STATE_UPDATER.getAndSet(this, State.DONE) != State.DONE) {
                if (failure != null) {
                    downstream.onFailure(failure);
                } else {
                    downstream.onCompletion();
                }
            }
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                state = State.DONE;
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            } else {
                Subscriptions.add(DEMAND_UPDATER, this, n);
                stateLock.lock();
                switch (state) {
                    case EMITTING:
                    case EMITTING_FINAL:
                        stateLock.unlock();
                        if (innerUpstream != null) {
                            innerUpstream.request(n);
                        }
                        break;
                    case READY:
                        state = State.PUBLISHER_REQUESTED;
                        stateLock.unlock();
                        mainUpstream.request(1L);
                        break;
                    default:
                        stateLock.unlock();
                        break;
                }
            }
        }

        @Override
        public void cancel() {
            mainUpstream.cancel();
            if (innerUpstream != null) {
                innerUpstream.cancel();
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

        private class InnerSubscriber implements MultiSubscriber<O>, ContextSupport {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                innerOnSubscribe(subscription);
            }

            @Override
            public void onItem(O item) {
                innerOnItem(item);
            }

            @Override
            public void onFailure(Throwable failure) {
                innerOnFailure(failure);
            }

            @Override
            public void onCompletion() {
                innerOnCompletion();
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
