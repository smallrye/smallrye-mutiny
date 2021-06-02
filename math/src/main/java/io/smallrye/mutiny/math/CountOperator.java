package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Count operator emitting the current count.
 * Everytime it gets an item from upstream, it emits the <em>count</em>.
 * If the stream emits the completion event without having emitting any item before, 0 is emitted, followed by the
 * completion event.
 * If the upstream emits a failure, the failure is propagated.
 *
 * @param <T> type of the incoming items.
 */
public class CountOperator<T> implements Function<Multi<T>, Multi<Long>> {

    private final AtomicLong count = new AtomicLong();

    @Override
    public Multi<Long> apply(Multi<T> multi) {
        return multi
                .onItem().transform(x -> count.incrementAndGet())
                .onCompletion().ifEmpty().continueWith(0L);
    }
}
