package mutiny.zero.internal;

import org.reactivestreams.Subscriber;

public class DroppingTube<T> extends TubeBase<T> {

    protected DroppingTube(Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        }
    }
}
