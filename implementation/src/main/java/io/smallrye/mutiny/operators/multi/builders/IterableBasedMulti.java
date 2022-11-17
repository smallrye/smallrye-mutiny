package io.smallrye.mutiny.operators.multi.builders;

import java.util.Iterator;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class IterableBasedMulti<T> extends AbstractMulti<T> {

    private final Iterable<? extends T> source;

    public IterableBasedMulti(Iterable<? extends T> source) {
        this.source = source;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "subscriber");
        Iterator<? extends T> iterator;

        try {
            iterator = source.iterator();
        } catch (Throwable e) {
            Subscriptions.fail(downstream, e);
            return;
        }

        subscribe(downstream, iterator);
    }

    public static <T> void subscribe(MultiSubscriber<? super T> downstream, Iterator<? extends T> iterator) {
        boolean hasNext;
        try {
            hasNext = iterator.hasNext();
        } catch (Throwable e) {
            Subscriptions.fail(downstream, e);
            return;
        }

        if (!hasNext) {
            Subscriptions.complete(downstream);
            return;
        }
        downstream.onSubscribe(new IteratorSubscription<T>(downstream, iterator));
    }

    private static final class IteratorSubscription<T> implements Subscription {

        private final Iterator<? extends T> iterator;
        private final MultiSubscriber<? super T> downstream;

        private volatile boolean cancelled;
        private final AtomicLong requested = new AtomicLong();

        IteratorSubscription(MultiSubscriber<? super T> downstream, Iterator<? extends T> iterator) {
            this.downstream = downstream;
            this.iterator = iterator;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0L) {
                    if (n == Long.MAX_VALUE) {
                        fastPath();
                    } else {
                        slowPath(n);
                    }
                }
            } else {
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
        }

        private void fastPath() {
            for (;;) {
                if (cancelled) {
                    return;
                }

                T t;

                try {
                    t = iterator.next();
                } catch (Throwable ex) {
                    downstream.onFailure(ex);
                    return;
                }

                if (cancelled) {
                    return;
                }

                if (t == null) {
                    downstream.onFailure(new NullPointerException("Iterator.next() returned a null value"));
                    return;
                } else {
                    downstream.onItem(t);
                }

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = iterator.hasNext();
                } catch (Throwable ex) {
                    downstream.onFailure(ex);
                    return;
                }

                if (!b) {
                    if (!cancelled) {
                        downstream.onCompletion();
                    }
                    return;
                }
            }
        }

        private void slowPath(long r) {
            long e = 0L;
            for (;;) {

                while (e != r) {

                    if (cancelled) {
                        return;
                    }

                    T t;

                    try {
                        t = iterator.next();
                    } catch (Throwable ex) {
                        downstream.onFailure(ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }

                    if (t == null) {
                        downstream.onFailure(new NullPointerException("Iterator.next() returned a null value"));
                        return;
                    } else {
                        downstream.onItem(t);
                    }

                    if (cancelled) {
                        return;
                    }

                    boolean b;

                    try {
                        b = iterator.hasNext();
                    } catch (Throwable ex) {
                        downstream.onFailure(ex);
                        return;
                    }

                    if (!b) {
                        if (!cancelled) {
                            downstream.onCompletion();
                        }
                        return;
                    }

                    e++;
                }

                r = requested.get();
                if (e == r) {
                    r = requested.addAndGet(-e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }

    }
}
