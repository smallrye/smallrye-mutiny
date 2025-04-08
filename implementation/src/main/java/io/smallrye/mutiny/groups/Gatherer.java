package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Tuple2;

/**
 * A Gatherer operator transforms a stream of items by accumulating them into an accumulator and extracting
 * items from that accumulator when certain conditions are met.
 *
 * @param <I> the type of the items emitted by the upstream
 * @param <ACC> the type of the accumulator
 * @param <O> the type of the items emitted to the downstream
 */
public interface Gatherer<I, ACC, O> {

    /**
     * Creates a new accumulator.
     *
     * @return a new accumulator
     */
    ACC accumulator();

    /**
     * Accumulates an item into the accumulator.
     *
     * @param accumulator the current accumulator
     * @param item the item to accumulate
     * @return the updated accumulator
     */
    ACC accumulate(ACC accumulator, I item);

    /**
     * Extracts an item from the accumulator.
     *
     * @param accumulator the current accumulator
     * @param upstreamCompleted whether the upstream has completed
     * @return an Optional containing a Tuple2 with the updated accumulator and the extracted item, or an empty Optional if no
     *         item can be extracted
     */
    Optional<Tuple2<ACC, O>> extract(ACC accumulator, boolean upstreamCompleted);

    /**
     * Finalizes the accumulator and extracts the final item, if any.
     * This method is called when the upstream has completed and no more items can be extracted using the extract method.
     *
     * @param accumulator the current accumulator
     * @return an Optional containing the final item, or an empty Optional if no final item can be extracted
     */
    Optional<O> finalize(ACC accumulator);

    /**
     * Builder for creating a {@link Gatherer}.
     *
     * @param <I> the type of the items emitted by the upstream
     */
    class Builder<I> {

        /**
         * Specifies the initial accumulator supplier.
         * <p>
         * The initial accumulator supplier is used to create a new accumulator.
         *
         * @param initialAccumulatorSupplier the supplier for the initial accumulator
         * @param <ACC> the type of the accumulator
         * @return the next step in the builder
         */
        @CheckReturnValue
        public <ACC> InitialAccumulatorStep<I, ACC> into(Supplier<ACC> initialAccumulatorSupplier) {
            nonNull(initialAccumulatorSupplier, "initialAccumulatorSupplier");
            return new InitialAccumulatorStep<>(initialAccumulatorSupplier);
        }
    }

    /**
     * The first step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream {@link Multi}
     * @param <ACC> the type of the accumulator
     */
    class InitialAccumulatorStep<I, ACC> {
        private final Supplier<ACC> initialAccumulatorSupplier;

        private InitialAccumulatorStep(Supplier<ACC> initialAccumulatorSupplier) {
            this.initialAccumulatorSupplier = initialAccumulatorSupplier;
        }

        /**
         * Specifies the accumulator function.
         * <p>
         * The accumulator function is used to accumulate the items emitted by the upstream.
         *
         * @param accumulator the accumulator function, which takes the current accumulator and the item emitted by the
         *        upstream, and returns the new accumulator
         * @return the next step in the builder
         */
        @CheckReturnValue
        public ExtractStep<I, ACC> accumulate(BiFunction<ACC, I, ACC> accumulator) {
            nonNull(accumulator, "accumulator");
            return new ExtractStep<>(initialAccumulatorSupplier, accumulator);
        }
    }

    /**
     * The second step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream {@link Multi}
     * @param <ACC> the type of the accumulator
     */
    class ExtractStep<I, ACC> {
        private final Supplier<ACC> initialAccumulatorSupplier;
        private final BiFunction<ACC, I, ACC> accumulator;

        private ExtractStep(Supplier<ACC> initialAccumulatorSupplier, BiFunction<ACC, I, ACC> accumulator) {
            this.initialAccumulatorSupplier = initialAccumulatorSupplier;
            this.accumulator = accumulator;
        }

        /**
         * Specifies the extractor function.
         * <p>
         * The extractor function is used to extract the items from the accumulator.
         * When the extractor function returns an empty {@link Optional}, no value is emitted.
         * When the extractor function returns a non-empty {@link Optional}, the value is emitted, and the accumulator is
         * updated.
         * This is done by returning a {@link Tuple2} containing the new accumulator and the value to emit.
         *
         * @param extractor the extractor function, which takes the current accumulator and returns an {@link Optional}
         *        containing a {@link Tuple2} with the new accumulator and the value to emit
         * @param <O> the type of the value to emit
         * @return the next step in the builder
         */
        @CheckReturnValue
        public <O> FinalizerStep<I, ACC, O> extract(BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor) {
            nonNull(extractor, "extractor");
            return new FinalizerStep<>(initialAccumulatorSupplier, accumulator, extractor);
        }
    }

    /**
     * The third step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream
     * @param <ACC> the type of the accumulator
     * @param <O> the type of the items emitted to the downstream
     */
    class FinalizerStep<I, ACC, O> {
        private final Supplier<ACC> initialAccumulatorSupplier;
        private final BiFunction<ACC, I, ACC> accumulator;
        private final BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor;

        private FinalizerStep(Supplier<ACC> initialAccumulatorSupplier,
                BiFunction<ACC, I, ACC> accumulator,
                BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor) {
            this.initialAccumulatorSupplier = initialAccumulatorSupplier;
            this.accumulator = accumulator;
            this.extractor = extractor;
        }

        /**
         * Specifies the finalizer function.
         * <p>
         * The finalizer function is used to emit the final value upon completion of the upstream and when there are no more
         * items that can be extracted from the accumulator.
         * When the finalizer function returns an empty {@link Optional}, no value is emitted before the completion signal.
         * When the finalizer function returns a non-empty {@link Optional}, the value is emitted before the completion signal.
         *
         * @param finalizer the finalizer function, which takes the current accumulator and returns an {@link Optional}
         *        containing the value to emit before the completion signal, if any
         * @return the gathering {@link Multi}
         */
        @CheckReturnValue
        public Gatherer<I, ACC, O> finalize(Function<ACC, Optional<O>> finalizer) {
            nonNull(finalizer, "finalizer");
            return Gatherers.of(initialAccumulatorSupplier, accumulator, extractor, finalizer);
        }
    }

}
