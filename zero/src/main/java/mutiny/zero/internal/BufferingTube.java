package mutiny.zero.internal;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.reactivestreams.Subscriber;

public class BufferingTube<T> extends TubeBase<T> {

    private final BlockingDeque<T> overflowQueue;
    private boolean delayedComplete = false;

    public BufferingTube(Subscriber<? super T> subscriber, int bufferSize) {
        super(subscriber);
        overflowQueue = new LinkedBlockingDeque<>(bufferSize);
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        } else if (!overflowQueue.offer(item)) {
            fail(new IllegalStateException(
                    "The following item cannot be propagated because there is no demand and the overflow buffer is full: "
                            + item));
        }
    }

    @Override
    public void request(long n) {
        if (cancelled) {
            return;
        }
        if (n <= 0L) {
            fail(Helper.negativeRequest(n));
        } else {

            if (overflowQueue.isEmpty()) {
                super.request(n);
                return;
            }

            long remaining = n;
            T bufferedItem;
            do {
                bufferedItem = overflowQueue.poll();
                if (bufferedItem != null) {
                    dispatchQueue.offer(bufferedItem);
                    remaining--;
                }
            } while (bufferedItem != null && remaining > 0L);

            Helper.add(requested, n);
            requestConsumer.accept(n);

            completed = delayedComplete && overflowQueue.isEmpty();
        }

        drainLoop();
    }

    @Override
    public void complete() {
        if (overflowQueue.isEmpty()) {
            super.complete();
        } else {
            delayedComplete = true;
        }
    }
}
