package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.Gatherer.Extraction;
import io.smallrye.mutiny.operators.multi.MultiGather;

/**
 * A builder to gather items emitted by a {@link Multi} into an accumulator.
 *
 * @param <I> the type of the items emitted by the upstream {@link Multi}
 */
@Experimental("This API is still being designed and may change in the future")
public class MultiOnItemGather<I> {

    private final Multi<I> upstream;

    public MultiOnItemGather(Multi<I> upstream) {
        this.upstream = upstream;
    }

    /**
     * Specifies the initial accumulator supplier.
     * <p>
     * The accumulator is used to accumulate the items emitted by the upstream.
     *
     * @param initialAccumulatorSupplier the initial accumulator supplier, the returned value cannot be {@code null}
     * @param <ACC> the type of the accumulator
     * @return the next step in the builder
     */
    @CheckReturnValue
    public <ACC> InitialAccumulatorStep<I, ACC> into(Supplier<ACC> initialAccumulatorSupplier) {
        nonNull(initialAccumulatorSupplier, "initialAccumulatorSupplier");
        return new InitialAccumulatorStep<>(upstream, initialAccumulatorSupplier);
    }

    /**
     * The first step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream {@link Multi}
     * @param <ACC> the type of the accumulator
     */
    public static class InitialAccumulatorStep<I, ACC> {
        private final Multi<I> upstream;
        private final Supplier<ACC> initialAccumulatorSupplier;

        private InitialAccumulatorStep(Multi<I> upstream, Supplier<ACC> initialAccumulatorSupplier) {
            this.upstream = upstream;
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
            return new ExtractStep<>(upstream, initialAccumulatorSupplier, accumulator);
        }
    }

    /**
     * The second step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream {@link Multi}
     * @param <ACC> the type of the accumulator
     */
    public static class ExtractStep<I, ACC> {
        private final Multi<I> upstream;
        private final Supplier<ACC> initialAccumulatorSupplier;
        private final BiFunction<ACC, I, ACC> accumulator;

        private ExtractStep(Multi<I> upstream, Supplier<ACC> initialAccumulatorSupplier, BiFunction<ACC, I, ACC> accumulator) {
            this.upstream = upstream;
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
         * This is done by returning a {@link Extraction} containing the new accumulator and the value to emit.
         *
         * @param extractor the extractor function, which takes the current accumulator and returns an {@link Optional}
         *        containing a {@link Extraction} with the new accumulator and the value to emit
         * @param <O> the type of the value to emit
         * @return the next step in the builder
         */
        @CheckReturnValue
        public <O> FinalizerStep<I, ACC, O> extract(BiFunction<ACC, Boolean, Optional<Extraction<ACC, O>>> extractor) {
            nonNull(extractor, "extractor");
            return new FinalizerStep<>(upstream, initialAccumulatorSupplier, accumulator, extractor);
        }
    }

    /**
     * The last step in the builder to gather items emitted by a {@link Multi} into an accumulator.
     *
     * @param <I> the type of the items emitted by the upstream {@link Multi}
     * @param <ACC> the type of the accumulator
     * @param <O> the type of the values to emit
     */
    public static class FinalizerStep<I, ACC, O> {
        private final Multi<I> upstream;
        private final Supplier<ACC> initialAccumulatorSupplier;
        private final BiFunction<ACC, I, ACC> accumulator;
        private final BiFunction<ACC, Boolean, Optional<Extraction<ACC, O>>> extractor;

        private FinalizerStep(Multi<I> upstream,
                Supplier<ACC> initialAccumulatorSupplier,
                BiFunction<ACC, I, ACC> accumulator,
                BiFunction<ACC, Boolean, Optional<Extraction<ACC, O>>> extractor) {
            this.upstream = upstream;
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
        public Multi<O> finalize(Function<ACC, Optional<O>> finalizer) {
            nonNull(finalizer, "finalizer");
            return new MultiGather<>(upstream, Gatherers.of(initialAccumulatorSupplier, accumulator, extractor, finalizer));
        }
    }
}