package mutiny.zero.internal;

import org.reactivestreams.Subscriber;

public class ErrorTube<T> extends TubeBase<T> {

    protected ErrorTube(Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        } else {
            fail(new IllegalStateException("The following item cannot be propagated because there is no demand: " + item));
        }
    }
}
