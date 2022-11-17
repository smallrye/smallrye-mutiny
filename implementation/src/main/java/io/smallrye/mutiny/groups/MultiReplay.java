package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.concurrent.Flow;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.replay.ReplayOperator;

/**
 * Group to configure replaying a {@link Multi} to multiple subscribers.
 */
public class MultiReplay {

    private long numberOfItemsToReplay = Long.MAX_VALUE;

    /**
     * Limit the number of items each new subscriber gets.
     * The default is to replay all events.
     *
     * @param numberOfItemsToReplay a strictly positive number of items to be replayed, where {@code Long.MAX_VALUE} means
     *        replaying all events
     * @return this group
     */
    @CheckReturnValue
    public MultiReplay upTo(long numberOfItemsToReplay) {
        this.numberOfItemsToReplay = positive(numberOfItemsToReplay, "numberOfItemsToReplay");
        return this;
    }

    /**
     * Create a replay {@link Multi}.
     * <p>
     * Replaying work as follows.
     * <ol>
     * <li>The provided {@code upstream} {@link Multi} is turned into a hot-stream as it gets requested {@code Long.MAX_VALUE}
     * elements.
     * This happens at the first subscription request. Note that {@code upstream} will never be cancelled.</li>
     * <li>Each new subscriber to this replay {@link Multi} is able to replay items at its own pace (back-pressure is
     * honored).</li>
     * <li>When the number of items to replay is limited using {@link #upTo(long)}, then a new subscriber gets to replay
     * starting from the current position in the upstream replay log.
     * When the number of elements to replay is unbounded, then a new subscriber replays from the start.</li>
     * <li>All current and late subscribers observe terminal completion / error signals.</li>
     * <li>Items are pushed synchronously to subscribers when they call {@link Flow.Subscription#request(long)}
     * and there are enough elements to satisfy a part of the demand.
     * Otherwise items are pushed from the upstream to all subscribers with an outstanding demand.</li>
     * </ol>
     * <p>
     * Replaying a large number of elements can be costly, as items have to be kept in-memory.
     * It is <strong>NOT</strong> recommended using this operator with unbounded streams, especially as they can't be canceled
     * (the subscribers
     * can cancel replays, though).
     * In such cases and especially when you have to keep replay data around for a long time then some eventing middleware might
     * be a better fit.
     *
     * @param upstream the {@link Multi} to replay, must not be {@code null}
     * @param <T> the items type
     * @return a replaying {@link Multi}
     */
    @CheckReturnValue
    public <T> Multi<T> ofMulti(Multi<T> upstream) {
        return new ReplayOperator<>(nonNull(upstream, "upstream"), numberOfItemsToReplay);
    }

    /**
     * Create a replay {@link Multi} with some seed elements inserted before the provided {@link Multi} items.
     * <p>
     * The behavior is that of {@link #ofMulti(Multi)}, except that the items from {@code seed} are prepended to those from
     * {@code upstream} in the replay log.
     *
     * @param seed the seed elements, must not be {@code null}, must not contain any {@code null} element
     * @param upstream the {@link Multi} to replay, must not be {@code null}
     * @param <T> the items type
     * @return a replaying {@link Multi}
     * @see #ofMulti(Multi)
     */
    @CheckReturnValue
    public <T> Multi<T> ofSeedAndMulti(Iterable<T> seed, Multi<T> upstream) {
        return new ReplayOperator<>(nonNull(upstream, "upstream"), numberOfItemsToReplay, nonNull(seed, "seed"));
    }
}
