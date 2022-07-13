package io.smallrye.mutiny.operators.multi.builders;

import java.util.Iterator;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class StreamBasedMulti<T> extends AbstractMulti<T> {

    private final Supplier<? extends Stream<? extends T>> supplier;

    public StreamBasedMulti(Supplier<? extends Stream<? extends T>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        ParameterValidation.nonNullNpe(downstream, "subscriber");

        Stream<? extends T> stream;

        try {
            stream = supplier.get();
        } catch (Throwable e) {
            Subscriptions.fail(downstream, e);
            return;
        }

        if (stream == null) {
            Subscriptions.fail(downstream, new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
            return;
        }

        Iterator<? extends T> iterator;
        try {
            iterator = stream.iterator();

            if (!iterator.hasNext()) {
                Subscriptions.complete(downstream);
                closeQuietly(stream);
                return;
            }
        } catch (Throwable ex) {
            Subscriptions.fail(downstream, ex);
            closeQuietly(stream);
            return;
        }

        downstream.onSubscribe(new StreamSubscription<>(downstream, iterator, stream));
    }

    private static void closeQuietly(AutoCloseable source) {
        if (source == null) {
            return;
        }
        try {
            source.close();
        } catch (Throwable ex) {
            // ignore the exception
        }
    }

    private static class StreamSubscription<T> implements Subscription {

        private final Iterator<? extends T> iterator;
        private final AutoCloseable closeable;
        private final AtomicLong requested = new AtomicLong();
        private final MultiSubscriber<T> downstream;
        private volatile boolean cancelled;

        StreamSubscription(MultiSubscriber<T> downstream, Iterator<? extends T> iterator, AutoCloseable closeable) {
            this.iterator = iterator;
            this.closeable = closeable;
            this.downstream = downstream;
        }

        @Override
        public void request(long n) {
            if (n > 0) {
                if (Subscriptions.add(requested, n) == 0L) {
                    pull(n);
                }
            } else {
                downstream.onFailure(Subscriptions.getInvalidRequestException());
            }
        }

        public void pull(long n) {
            long emitted = 0L;
            for (;;) {
                if (cancelled) {
                    closeQuietly(closeable);
                    return;
                } else {
                    T item;
                    try {
                        item = iterator.next();
                        if (item == null) {
                            throw new NullPointerException("The stream iterator produced `null`");
                        }
                    } catch (Throwable ex) {
                        downstream.onFailure(ex);
                        cancelled = true;
                        continue;
                    }

                    downstream.onItem(item);

                    if (cancelled || handleCompletion()) {
                        continue;
                    }

                    if (++emitted != n) {
                        continue;
                    }
                }

                n = requested.get();
                if (emitted == n) {
                    if (requested.compareAndSet(n, 0L)) {
                        break;
                    }
                    n = requested.get();
                }
            }
        }

        private boolean handleCompletion() {
            try {
                if (!iterator.hasNext()) {
                    downstream.onCompletion();
                    cancelled = true;
                    return true;
                }
            } catch (Throwable ex) {
                downstream.onFailure(ex);
                cancelled = true;
                return true;
            }
            return false;
        }

        @Override
        public void cancel() {
            cancelled = true;
            request(1L);
        }
    }
}
