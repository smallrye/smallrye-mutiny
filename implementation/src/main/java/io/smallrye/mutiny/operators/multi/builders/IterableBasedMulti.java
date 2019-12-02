package io.smallrye.mutiny.operators.multi.builders;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;

public class IterableBasedMulti<T> extends AbstractMulti<T> {

    private final Iterable<? extends T> source;

    public IterableBasedMulti(Iterable<? extends T> source) {
        this.source = source;
    }

    @Override
    protected Publisher<T> publisher() {
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super T> downstream) {
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

    public static <T> void subscribe(Subscriber<? super T> downstream, Iterator<? extends T> it) {
        boolean hasNext;
        try {
            hasNext = it.hasNext();
        } catch (Throwable e) {
            Subscriptions.fail(downstream, e);
            return;
        }

        if (!hasNext) {
            Subscriptions.complete(downstream);
            return;
        }
        downstream.onSubscribe(new IteratorSubscription<T>(downstream, it));
    }

    abstract static class BaseRangeSubscription<T> implements Subscription {
        protected final Iterator<? extends T> iterator;
        protected final Subscriber<? super T> downstream;
        protected volatile boolean cancelled;
        protected boolean once;
        protected final AtomicLong requested = new AtomicLong();

        BaseRangeSubscription(Subscriber<? super T> downstream, Iterator<? extends T> iterator) {
            this.downstream = downstream;
            this.iterator = iterator;
        }

        @Override
        public final void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0L) {
                    if (n == Long.MAX_VALUE) {
                        fastPath();
                    } else {
                        slowPath(n);
                    }
                }
            } else {
                downstream.onError(Subscriptions.getInvalidRequestException());
            }
        }

        @Override
        public final void cancel() {
            cancelled = true;
        }

        abstract void fastPath();

        abstract void slowPath(long r);
    }

    static final class IteratorSubscription<T> extends BaseRangeSubscription<T> {

        IteratorSubscription(Subscriber<? super T> actual, Iterator<? extends T> it) {
            super(actual, it);
        }

        @Override
        void fastPath() {
            for (;;) {
                if (cancelled) {
                    return;
                }

                T t;

                try {
                    t = iterator.next();
                } catch (Throwable ex) {
                    downstream.onError(ex);
                    return;
                }

                if (cancelled) {
                    return;
                }

                if (t == null) {
                    downstream.onError(new NullPointerException("Iterator.next() returned a null value"));
                    return;
                } else {
                    downstream.onNext(t);
                }

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = iterator.hasNext();
                } catch (Throwable ex) {
                    downstream.onError(ex);
                    return;
                }

                if (!b) {
                    if (!cancelled) {
                        downstream.onComplete();
                    }
                    return;
                }
            }
        }

        @Override
        void slowPath(long r) {
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
                        downstream.onError(ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }

                    if (t == null) {
                        downstream.onError(new NullPointerException("Iterator.next() returned a null value"));
                        return;
                    } else {
                        downstream.onNext(t);
                    }

                    if (cancelled) {
                        return;
                    }

                    boolean b;

                    try {
                        b = iterator.hasNext();
                    } catch (Throwable ex) {
                        downstream.onError(ex);
                        return;
                    }

                    if (!b) {
                        if (!cancelled) {
                            downstream.onComplete();
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
