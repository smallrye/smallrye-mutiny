package mutiny.zero.internal;

import org.reactivestreams.Subscriber;

public class LatestTube<T> extends BufferingTubeBase<T> {

    public LatestTube(Subscriber<? super T> subscriber, int bufferSize) {
        super(subscriber, bufferSize);
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
