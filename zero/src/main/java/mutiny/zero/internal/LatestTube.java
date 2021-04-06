package mutiny.zero.internal;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import org.reactivestreams.Subscriber;

public class LatestTube<T> extends BufferingTubeBase<T> {

    private final LinkedBlockingDeque<T> overflowQueue;

    public LatestTube(Subscriber<? super T> subscriber, int bufferSize) {
        super(subscriber, bufferSize);
        overflowQueue = new LinkedBlockingDeque<>(bufferSize);
    }

    @Override
    Queue<T> overflowQueue() {
        return overflowQueue;
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        } else if (!overflowQueue.offer(item)) {
            overflowQueue.remove();
            overflowQueue.offer(item);
        }
    }
}
