package mutiny.zero.internal;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.reactivestreams.Subscriber;

public abstract class BufferingTubeBase<T> extends TubeBase<T> {

    protected final BlockingDeque<T> overflowQueue;
    protected boolean delayedComplete = false;

    public BufferingTubeBase(Subscriber<? super T> subscriber, int bufferSize) {
        super(subscriber);
        overflowQueue = new LinkedBlockingDeque<>(bufferSize);
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
