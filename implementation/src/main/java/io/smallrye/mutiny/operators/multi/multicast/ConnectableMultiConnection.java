package io.smallrye.mutiny.operators.multi.multicast;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class ConnectableMultiConnection implements Runnable, Consumer<Cancellable> {

    private static final Cancellable CANCELLED = () -> {
        // do nothing.
    };

    private final MultiReferenceCount<?> parent;
    private final MultiSubscriber<?> subscriber;
    private final AtomicReference<Cancellable> onCancellation = new AtomicReference<>();

    private Cancellable timer;
    private long subscriberCount;
    private boolean connected;

    ConnectableMultiConnection(MultiReferenceCount<?> parent, MultiSubscriber<?> subscriber) {
        this.parent = parent;
        this.subscriber = subscriber;
    }

    @Override
    public void run() {
        parent.timeout(this);
    }

    @Override
    public void accept(Cancellable action) {
        for (;;) {
            Cancellable current = onCancellation.get();
            if (current == CANCELLED) {
                if (action != null) {
                    action.cancel();
                }
            }
            if (onCancellation.compareAndSet(current, action)) {
                break;
            }
        }
    }

    public synchronized boolean shouldConnectAfterIncrement(int toBeReached) {
        subscriberCount = subscriberCount + 1;
        if (!connected && subscriberCount == toBeReached) {
            connected = true;
            return true;
        } else {
            return false;
        }
    }

    public long getSubscriberCount() {
        return subscriberCount;
    }

    public boolean isConnected() {
        return connected;
    }

    public void cancelTimerIf0() {
        boolean cancel;
        synchronized (this) {
            cancel = subscriberCount == 0L && timer != null;
        }
        if (cancel) {
            timer.cancel();
        }
    }

    public void cancel() {
        Cancellable current = onCancellation.getAndSet(CANCELLED);
        if (current != null && current != CANCELLED) {
            current.cancel();
        }
    }

    synchronized boolean decrementAndReached0() {
        if (subscriberCount == 1) {
            subscriberCount = 0;
            return true;
        } else {
            subscriberCount--;
            return false;
        }
    }

    synchronized long decrement() {
        subscriberCount = subscriberCount - 1;
        return subscriberCount;
    }

    synchronized void setTimer(Cancellable cancellable) {
        if (timer != null && timer != CANCELLED) {
            timer.cancel();
        }
        this.timer = cancellable;
    }

    public MultiSubscriber<?> getSubscriber() {
        return subscriber;
    }
}
