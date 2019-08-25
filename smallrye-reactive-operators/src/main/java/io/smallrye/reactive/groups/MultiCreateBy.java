package io.smallrye.reactive.groups;


public class MultiCreateBy {
    public static final MultiCreateBy INSTANCE = new MultiCreateBy();

    private MultiCreateBy() {
        // Avoid direct instantiation
    }

    /**
     * Creates a new instance of {@link io.smallrye.reactive.Multi} by concatenating several
     * {@link io.smallrye.reactive.Multi} or {@link org.reactivestreams.Publisher} instances.
     * <p>
     * The concatenation reads the streams in order and emits the items in order.
     *
     * @return the object to configure the concatenation
     */
    public MultiConcat concatenating() {
        return new MultiConcat(false, 128);
    }

    /**
     * Creates a new instance of {@link io.smallrye.reactive.Multi} by merging several
     * {@link io.smallrye.reactive.Multi} or {@link org.reactivestreams.Publisher} instances.
     * <p>
     * The concatenation reads the streams in parallel and emits the items as they come.
     *
     * @return the object to configure the merge
     */
    public MultiMerge merging() {
        return new MultiMerge(false, 128, 128);
    }

    /**
     * Creates a new instance of {@link io.smallrye.reactive.Multi} by associating / combining the items from different
     * streams ({@link io.smallrye.reactive.Multi} or {@link org.reactivestreams.Publisher}).
     * <p>
     * The resulting {@link io.smallrye.reactive.Multi} can:
     * <ul>
     * <li>collects an item of every observed streams and combine them. If one of the observed stream sends the
     * completion event, the event is propagated in the produced stream, and no other combination are emitted.</li>
     * <li>as soon as on of the observed stream emits an item, it combine it with the latest items for the other stream.
     * the completion event is sent when all the observed streams have completed (with a completion event).</li>
     * </ul>
     * <p>
     * The combination also allows to collect the failures and propagate a failure when all observed streams have completed
     * (or failed) instead of propagating the failure immediately.
     *
     *
     *
     * @return the object to configure the combination
     */
    public MultiItemCombination combining() {
        return new MultiItemCombination();
    }

}
