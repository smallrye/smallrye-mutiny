package io.smallrye.reactive.groups;


public class MultiCreateBy {
    public static final MultiCreateBy INSTANCE = new MultiCreateBy();

    private MultiCreateBy() {
        // Avoid direct instantiation
    }

    /**
     * Creates new instances of {@link io.smallrye.reactive.Multi} by concatenating several
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
     * Creates new instances of {@link io.smallrye.reactive.Multi} by merging several
     * {@link io.smallrye.reactive.Multi} or {@link org.reactivestreams.Publisher} instances.
     * <p>
     * The concatenation reads the streams in parallel and emits the items as they come.
     *
     * @return the object to configure the merge
     */
    public MultiMerge merging() {
        return new MultiMerge(false, 128, 128);
    }

    // TODO associating

}
