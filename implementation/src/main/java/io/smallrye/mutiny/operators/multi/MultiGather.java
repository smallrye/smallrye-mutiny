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
        // number of upstream requests in flight, each one backed by a unit of demand taken from `demand`
        private final AtomicLong reserved = new AtomicLong();
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
                    requestNextUpstreamItem();
                }
            }
        }

        // Extracted items are forwarded downstream as soon as upstream items arrive, so we must not have more upstream
        // items on their way than what the downstream asked for: request one item at a time, and only when a unit of
        // demand can be reserved for it.
        private void requestNextUpstreamItem() {
            if (reserved.get() > 0L) {
                return;
            }
            long current;
            do {
                current = demand.get();
                if (current <= 0L) {
                    return;
                }
            } while (current != Long.MAX_VALUE && !demand.compareAndSet(current, current - 1L));
            reserved.incrementAndGet();
            upstream.request(1L);
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
                    reserved.decrementAndGet();
                    downstream.onItem(value);
                    requestNextUpstreamItem();
                } else {
                    // nothing was extracted, the reserved unit of demand carries over to the next upstream item
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
            long unused = reserved.getAndSet(0L);
            if (unused > 0L) {
                Subscriptions.add(demand, unused);
            }
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
