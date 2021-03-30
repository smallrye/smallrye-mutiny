package mutiny.zero.internal;

import org.reactivestreams.Subscriber;

public class IgnoringTube<T> extends TubeBase<T> {

    public IgnoringTube(Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    protected void handleItem(T item) {
        dispatchQueue.offer(item);
        drainLoop();
    }
}
