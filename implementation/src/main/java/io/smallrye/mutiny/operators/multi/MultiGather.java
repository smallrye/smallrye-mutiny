package io.smallrye.mutiny.operators.multi;

import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.Gatherer;
import io.smallrye.mutiny.groups.Gatherer.Extraction;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiGather<I, ACC, O> extends AbstractMultiOperator<I, O> {

    private final Gatherer<I, ACC, O> gatherer;

    public MultiGather(Multi<? extends I> upstream, Gatherer<I, ACC, O> gatherer) {
        super(upstream);
        this.gatherer = gatherer;
    }

    @Override
    public void subscribe(MultiSubscriber<? super O> subscriber) {
        upstream.subscribe().withSubscriber(new MultiGatherProcessor(subscriber));
    }

    class MultiGatherProcessor extends MultiOperatorProcessor<I, O> {

        private ACC acc;
        private final AtomicLong demand = new AtomicLong();
        private volatile boolean upstreamHasCompleted;
        private final AtomicInteger drainWip = new AtomicInteger();

        public MultiGatherProcessor(MultiSubscriber<? super O> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            try {
                this.acc = gatherer.accumulator();
                if (this.acc == null) {
                    throw new NullPointerException("The initial accumulator cannot be null");
                }
            } catch (Throwable err) {
                downstream.onSubscribe(Subscriptions.CANCELLED);
                onFailure(err);
                return;
            }
            super.onSubscribe(subscription);
        }

        @Override
        public void request(long numberOfItems) {
            if (numberOfItems <= 0) {
                onFailure(new IllegalArgumentException("The number of items requested must be strictly positive"));
                return;
            }
            if (upstream != Subscriptions.CANCELLED) {
                Subscriptions.add(demand, numberOfItems);
                if (upstreamHasCompleted) {
                    drainRemainingElements();
                } else {
                    upstream.request(1L);
                }
            }
        }

        @Override
        public void onItem(I item) {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            try {
                acc = gatherer.accumulate(acc, item);
                if (acc == null) {
                    throw new NullPointerException("The accumulator returned a null value");
                }
                Optional<Extraction<ACC, O>> mapping = gatherer.extract(acc, false);
                if (mapping == null) {
                    throw new NullPointerException("The extractor returned a null value");
                }
                if (mapping.isPresent()) {
                    Extraction<ACC, O> result = mapping.get();
                    acc = result.nextAccumulator();
                    O value = result.nextItem();
                    if (acc == null) {
                        throw new NullPointerException("The extractor returned a null accumulator value");
                    }
                    if (value == null) {
                        throw new NullPointerException("The extractor returned a null value to emit");
                    }
                    long remaining = demand.decrementAndGet();
                    downstream.onItem(value);
                    if (remaining > 0L) {
                        upstream.request(1L);
                    }
                } else {
                    upstream.request(1L);
                }
            } catch (Throwable err) {
                onFailure(err);
            }
        }

        @Override
        public void onCompletion() {
            if (upstream == Subscriptions.CANCELLED) {
                return;
            }
            upstreamHasCompleted = true;
            drainRemainingElements();
        }

        private void drainRemainingElements() {
            if (drainWip.getAndIncrement() > 0) {
                return;
            }
            while (true) {
                long pending = demand.get();
                long emitted = 0L;
                while (emitted < pending) {
                    if (upstream == Subscriptions.CANCELLED) {
                        return;
                    }
                    try {
                        Optional<Extraction<ACC, O>> mapping = gatherer.extract(acc, true);
                        if (mapping == null) {
                            throw new NullPointerException("The extractor returned a null value");
                        }
                        if (mapping.isPresent()) {
                            Extraction<ACC, O> result = mapping.get();
                            acc = result.nextAccumulator();
                            O value = result.nextItem();
                            if (acc == null) {
                                throw new NullPointerException("The extractor returned a null accumulator value");
                            }
                            if (value == null) {
                                throw new NullPointerException("The extractor returned a null value to emit");
                            }
                            downstream.onItem(value);
                            emitted = emitted + 1L;
                        } else {
                            Optional<O> finalValue = gatherer.finalize(acc);
                            if (finalValue == null) {
                                throw new NullPointerException("The finalizer returned a null value");
                            }
                            this.upstream = Subscriptions.CANCELLED;
                            finalValue.ifPresent(o -> downstream.onItem(o));
                            downstream.onCompletion();
                            return;
                        }
                    } catch (Throwable err) {
                        onFailure(err);
                        return;
                    }
                }
                demand.addAndGet(-emitted);
                if (drainWip.decrementAndGet() == 0) {
                    return;
                }
            }
        }
    }
}
