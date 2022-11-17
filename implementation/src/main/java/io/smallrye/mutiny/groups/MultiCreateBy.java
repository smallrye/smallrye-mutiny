package io.smallrye.mutiny.groups;

import java.util.concurrent.Flow;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;

/**
 * Allows the creation of instances of {@link Multi} by merging/combining/concatenating multiple upstreams.
 */
public class MultiCreateBy {
    public static final MultiCreateBy INSTANCE = new MultiCreateBy();

    private MultiCreateBy() {
        // Avoid direct instantiation
    }

    /**
     * Creates a new instance of {@link Multi} by concatenating several
     * {@link Multi} or {@link Flow.Publisher} instances.
     * <p>
     * The concatenation reads the streams in order and emits the items in order.
     *
     * @return the object to configure the concatenation
     */
    @CheckReturnValue
    public MultiConcat concatenating() {
        return new MultiConcat(false);
    }

    /**
     * Creates a new instance of {@link Multi} by merging several
     * {@link Multi} or {@link Flow.Publisher} instances.
     * <p>
     * The concatenation reads the streams concurrently and emits the items as they come.
     *
     * @return the object to configure the merge
     */
    @CheckReturnValue
    public MultiMerge merging() {
        return new MultiMerge(false, 128, 128);
    }

    /**
     * Creates a new instance of {@link Multi} by associating / combining the items from different
     * streams ({@link Multi} or {@link Flow.Publisher}).
     * <p>
     * The resulting {@link Multi} can:
     * <ul>
     * <li>collects an item of every observed streams and combines them. If one of the observed stream sends the
     * completion event, the event is propagated in the produced stream, and no other combination are emitted.</li>
     * <li>as soon as on of the observed stream emits an item, it combines it with the latest items emitted by other stream.
     * the completion event is sent when all the observed streams have completed (with a completion event).</li>
     * </ul>
     * <p>
     * The combination also allows to collect the failures and propagates a failure when all observed streams have completed
     * (or failed) instead of propagating the failure immediately.
     *
     * @return the object to configure the combination
     */
    @CheckReturnValue
    public MultiItemCombination combining() {
        return new MultiItemCombination();
    }

    /**
     * Creates a new {@link Multi} by repeating a given function producing {@link io.smallrye.mutiny.Uni unis} or
     * {@link java.util.concurrent.CompletionStage Completion Stages}.
     *
     * @return the object to configure the repetition
     */
    @CheckReturnValue
    public MultiRepetition repeating() {
        return new MultiRepetition();
    }

    /**
     * Creates a new {@link Multi} that replays elements from another {@link Multi} to any number of current and late
     * subscribers.
     *
     * @return the object to configure the replay behavior
     */
    @CheckReturnValue
    public MultiReplay replaying() {
        return new MultiReplay();
    }
}
