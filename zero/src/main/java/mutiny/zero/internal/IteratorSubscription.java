package mutiny.zero.internal;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class IteratorSubscription<T> implements Subscription {

    private final Subscriber<? super T> subscriber;

    private volatile boolean cancelled = false;
    private final Iterator<T> iterator;
    private final AtomicLong requested = new AtomicLong();

    IteratorSubscription(Iterator<T> iterator, Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        this.iterator = iterator;
    }

    @Override
    public void request(long n) {
        if (n <= 0L) {
            cancel();
            subscriber.onError(Helper.negativeRequest(n));
            return;
        }
        if (Helper.add(requested, n) == 0) {
            if (n == Long.MAX_VALUE) {
                deliverAll();
            } else {
                deliver(n);
            }
        }
    }

    private void deliver(long n) {
        long emitted = 0;
        for (;;) {

            if (cancelled) {
                return;
            }

            while (emitted != n) {
                if (iterator.hasNext()) {
                    T next;
                    try {
                        next = iterator.next();
                    } catch (Throwable err) {
                        subscriber.onError(err);
                        return;
                    }
                    if (next == null) {
                        cancelled = true;
                        subscriber.onError(new NullPointerException("The iterable has a null value"));
                        return;
                    }
                    subscriber.onNext(next);
                    emitted++;
                } else {
                    subscriber.onComplete();
                    return;
                }
            }

            n = requested.get();
            if (n == emitted) {
                n = requested.addAndGet(-emitted);
                if (n == 0L) {
                    return;
                }
                emitted = 0L;
            }
        }
    }

    private void deliverAll() {
        while (iterator.hasNext()) {
            if (cancelled) {
                return;
            }
            T next = iterator.next();
            if (next == null) {
                cancelled = true;
                subscriber.onError(new NullPointerException("The iterable has a null value"));
                return;
            }
            subscriber.onNext(next);
        }
        if (cancelled) {
            return;
        }
        cancelled = true;
        subscriber.onComplete();
    }

    @Override
    public void cancel() {
        cancelled = true;
    }
}
