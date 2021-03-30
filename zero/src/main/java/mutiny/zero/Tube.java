package mutiny.zero;

import java.util.function.LongConsumer;

public interface Tube<T> {

    Tube<T> send(T item);

    void fail(Throwable err);

    void complete();

    boolean cancelled();

    long outstandingRequests();

    Tube<T> whenCancelled(Runnable action);

    Tube<T> whenTerminates(Runnable action);

    Tube<T> whenRequested(LongConsumer consumer);

}
