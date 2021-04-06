package mutiny.zero.internal;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.reactivestreams.Subscriber;

public class UnbounbedBufferingTube<T> extends BufferingTubeBase<T> {

    private final ConcurrentLinkedQueue<T> overflowQueue;

    public UnbounbedBufferingTube(Subscriber<? super T> subscriber) {
        super(subscriber, -1);
        overflowQueue = new ConcurrentLinkedQueue<>();
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
        } else {
            overflowQueue.offer(item);
        }
    }
}
