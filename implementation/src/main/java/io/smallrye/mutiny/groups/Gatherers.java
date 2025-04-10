package io.smallrye.mutiny.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.smallrye.mutiny.tuples.Tuple2;

/**
 * Factory interface for creating {@link Gatherer} instances.
 * This interface provides various static methods to create different types of gatherers.
 */
public interface Gatherers {

    /**
     * Creates a new {@link Gatherer} with the specified components.
     *
     * @param initialAccumulatorSupplier the supplier for the initial accumulator
     * @param accumulatorFunction the function to accumulate items into the accumulator
     * @param extractor the function to extract items from the accumulator
     * @param finalizer the function to extract the final item from the accumulator
     * @param <I> the type of the items emitted by the upstream
     * @param <ACC> the type of the accumulator
     * @param <O> the type of the items emitted to the downstream
     * @return a new {@link Gatherer}
     */
    static <I, ACC, O> Gatherer<I, ACC, O> of(Supplier<ACC> initialAccumulatorSupplier,
            BiFunction<ACC, I, ACC> accumulatorFunction,
            BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor,
            Function<ACC, Optional<O>> finalizer) {
        return new DefaultGatherer<>(initialAccumulatorSupplier, accumulatorFunction, extractor, finalizer);
    }

    /**
     * Creates a new {@link Gatherer} that performs a scan operation.
     * The scan operation applies a function to each item emitted by the upstream, using the result of the previous
     * application as the first argument to the function. The initial value is provided by the initialAccumulatorSupplier.
     * <p>
     * Each intermediate result is emitted downstream.
     *
     * @param initialAccumulatorSupplier the supplier for the initial accumulator
     * @param accumulatorFunction the function to accumulate items
     * @param <I> the type of the items emitted by the upstream and downstream
     * @return a new {@link Gatherer} that performs a scan operation
     */
    static <I> Gatherer<I, I, I> scan(Supplier<I> initialAccumulatorSupplier, BiFunction<I, I, I> accumulatorFunction) {
        return of(initialAccumulatorSupplier, accumulatorFunction,
                (acc, done) -> done ? Optional.empty() : Optional.of(Tuple2.of(acc, acc)), Optional::of);
    }

    /**
     * Creates a new {@link Gatherer} that performs a fold operation.
     * The fold operation applies a function to each item emitted by the upstream, using the result of the previous
     * application as the first argument to the function. The initial value is provided by the initialAccumulatorSupplier.
     * <p>
     * Only emits the final result when the upstream completes.
     *
     * @param initialAccumulatorSupplier the supplier for the initial accumulator
     * @param accumulatorFunction the function to accumulate items
     * @param <I> the type of the items emitted by the upstream and downstream
     * @return a new {@link Gatherer} that performs a fold operation
     */
    static <I> Gatherer<I, I, I> fold(Supplier<I> initialAccumulatorSupplier, BiFunction<I, I, I> accumulatorFunction) {
        return of(initialAccumulatorSupplier, accumulatorFunction, (acc, done) -> Optional.empty(), Optional::of);
    }

    /**
     * Creates a new {@link Gatherer} that performs a windowing operation.
     * The windowing operation collects items emitted by the upstream into non-overlapping windows of the specified size.
     * When a window is full, it is emitted downstream and a new window is started.
     * If the upstream completes before a window is full, the current window is emitted if it is not empty.
     *
     * @param size the size of the window
     * @param <I> the type of the items emitted by the upstream
     * @return a new {@link Gatherer} that performs a windowing operation
     */
    static <I> Gatherer<I, List<I>, List<I>> window(int size) {
        return of(ArrayList::new, (acc, next) -> {
            acc.add(next);
            return acc;
        }, (acc, completed) -> {
            if (acc.size() == size) {
                return Optional.of(Tuple2.of(new ArrayList<>(), new ArrayList<>(acc)));
            }
            return Optional.empty();
        }, acc -> acc.isEmpty()
                ? Optional.empty()
                : Optional.of(acc));
    }

    /**
     * Creates a new {@link Gatherer} that performs a sliding window operation.
     * The sliding window operation collects items emitted by the upstream into overlapping windows of the specified size.
     * When a window is full, it is emitted downstream and a new window is started with all but the first item from the previous
     * window.
     * If the upstream completes before a window is full, the current window is emitted if it is not empty.
     *
     * @param size the size of the window
     * @param <I> the type of the items emitted by the upstream
     * @return a new {@link Gatherer} that performs a sliding window operation
     */
    static <I> Gatherer<I, List<I>, List<I>> slidingWindow(int size) {
        return of(ArrayList::new, (acc, item) -> {
            acc.add(item);
            return acc;
        }, (acc, completed) -> {
            if (acc.size() == size) {
                return Optional.of(Tuple2.of(acc.stream().skip(1).collect(Collectors.toList()), new ArrayList<>(acc)));
            }
            return Optional.empty();
        }, acc -> acc.isEmpty()
                ? Optional.empty()
                : Optional.of(acc));
    }

    /**
     * Default implementation of the {@link Gatherer} interface.
     *
     * @param <I> the type of the items emitted by the upstream
     * @param <ACC> the type of the accumulator
     * @param <O> the type of the items emitted to the downstream
     */
    class DefaultGatherer<I, ACC, O> implements Gatherer<I, ACC, O> {

        private final Supplier<ACC> initialAccumulatorSupplier;
        private final BiFunction<ACC, I, ACC> accumulatorFunction;
        private final BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor;
        private final Function<ACC, Optional<O>> finalizer;

        public DefaultGatherer(Supplier<ACC> initialAccumulatorSupplier,
                BiFunction<ACC, I, ACC> accumulatorFunction,
                BiFunction<ACC, Boolean, Optional<Tuple2<ACC, O>>> extractor,
                Function<ACC, Optional<O>> finalizer) {
            this.initialAccumulatorSupplier = initialAccumulatorSupplier;
            this.accumulatorFunction = accumulatorFunction;
            this.extractor = extractor;
            this.finalizer = finalizer;
        }

        @Override
        public ACC accumulator() {
            return initialAccumulatorSupplier.get();
        }

        @Override
        public ACC accumulate(ACC accumulator, I item) {
            return accumulatorFunction.apply(accumulator, item);
        }

        @Override
        public Optional<Tuple2<ACC, O>> extract(ACC accumulator, boolean upstreamCompleted) {
            return extractor.apply(accumulator, upstreamCompleted);
        }

        @Override
        public Optional<O> finalize(ACC accumulator) {
            return finalizer.apply(accumulator);
        }
    }

    /**
     * Creates a new {@link Gatherer} builder.
     *
     * @param <I> the type of the items emitted by the upstream
     * @return the builder
     */
    static <I> Gatherer.Builder<I> builder() {
        return new Gatherer.Builder<>();
    }

}
