package io.smallrye.mutiny.operators.multi.replay;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class ReplayOperator<T> extends AbstractMulti<T> {

    private final Multi<T> upstream;

    private final AppendOnlyReplayList replayList;

    private final AtomicBoolean upstreamSubscriptionRequested = new AtomicBoolean();
    private volatile Subscription upstreamSubscription = null;
    private final CopyOnWriteArrayList<ReplaySubscription> subscriptions = new CopyOnWriteArrayList<>();

    public ReplayOperator(Multi<T> upstream, long numberOfItemsToReplay) {
        this.upstream = upstream;
        this.replayList = new AppendOnlyReplayList(numberOfItemsToReplay);
    }

    public ReplayOperator(Multi<T> upstream, long numberOfItemsToReplay, Iterable<T> seed) {
        this.upstream = upstream;
        this.replayList = new AppendOnlyReplayList(numberOfItemsToReplay, seed);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        if (upstreamSubscriptionRequested.compareAndSet(false, true)) {
            upstream.subscribe(new UpstreamSubscriber(subscriber));
        }
        ReplaySubscription replaySubscription = new ReplaySubscription(subscriber);
        subscriber.onSubscribe(replaySubscription);
        subscriptions.add(replaySubscription);
    }

    private class ReplaySubscription implements Subscription {

        private final MultiSubscriber<? super T> downstream;
        private final AtomicLong demand = new AtomicLong();
        private volatile boolean done = false;
        private final AppendOnlyReplayList.Cursor cursor;

        private ReplaySubscription(MultiSubscriber<? super T> downstream) {
            this.downstream = downstream;
            this.cursor = replayList.newCursor();
            this.cursor.hasNext(); // Try to catch the replay stream at subscription time if ready
        }

        @Override
        public void request(long n) {
            if (done) {
                return;
            }
            if (n <= 0) {
                cancel();
                downstream.onFailure(Subscriptions.getInvalidRequestException());
                return;
            }
            Subscriptions.add(demand, n);
            if (cursor.hasNext()) {
                drain();
            }
        }

        @Override
        public void cancel() {
            done = true;
            subscriptions.remove(this);
        }

        private final AtomicInteger wip = new AtomicInteger();

        @SuppressWarnings("unchecked")
        private void drain() {
            if (done) {
                return;
            }
            if (wip.getAndIncrement() > 0) {
                return;
            }
            while (true) {
                if (done) {
                    return;
                }
                long max = demand.get();
                long emitted = 0;
                while (emitted < max && cursor.hasNext()) {
                    if (done) {
                        return;
                    }
                    cursor.moveToNext();
                    if (cursor.hasReachedCompletion()) {
                        cancel();
                        cursor.readCompletion();
                        downstream.onComplete();
                        return;
                    }
                    if (cursor.hasReachedFailure()) {
                        cancel();
                        downstream.onFailure(cursor.readFailure());
                        return;
                    }
                    T item = (T) cursor.read();
                    assert item != null; // Invariant enforced by AppendOnlyReplayList
                    downstream.onItem(item);
                    emitted++;
                }
                demand.addAndGet(-emitted);
                if (wip.decrementAndGet() == 0) {
                    return;
                }
            }
        }
    }

    private class UpstreamSubscriber implements MultiSubscriber<T>, ContextSupport {

        private final MultiSubscriber<? super T> initialSubscriber;

        public UpstreamSubscriber(MultiSubscriber<? super T> initialSubscriber) {
            this.initialSubscriber = initialSubscriber;
        }

        @Override
        public void onItem(T item) {
            replayList.push(item);
            triggerDrainLoops();
        }

        @Override
        public void onFailure(Throwable failure) {
            replayList.pushFailure(failure);
            markAsDone();
            triggerDrainLoops();
        }

        @Override
        public void onCompletion() {
            replayList.pushCompletion();
            markAsDone();
            triggerDrainLoops();
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            upstreamSubscription = subscription;
            upstreamSubscription.request(Long.MAX_VALUE);
        }

        @Override
        public Context context() {
            if (initialSubscriber instanceof ContextSupport) {
                return ((ContextSupport) initialSubscriber).context();
            } else {
                return Context.empty();
            }
        }

        private void triggerDrainLoops() {
            subscriptions.forEach(ReplaySubscription::drain);
        }

        private void markAsDone() {
            upstreamSubscription = Subscriptions.CANCELLED;
        }
    }
}
