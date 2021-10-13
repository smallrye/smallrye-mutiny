package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.CheckReturnValue;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.builders.UniJoinAll;
import io.smallrye.mutiny.operators.uni.builders.UniJoinFirst;

/**
 * Join multiple {@link Uni}.
 */
@Experimental("New API based on observations that Uni.combine() is often used with homogeneous types, and combination often just a mapping to a collection.")
public class UniJoin {

    public static final UniJoin SHARED_INSTANCE = new UniJoin();

    private UniJoin() {
        // Do nothing
    }

    /**
     * Join multiple {@link Uni} references and emit a list of values, this is a convenience delegate method for
     * {@link #all(List)}.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, must not contain any {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> JoinAllStrategy<T> all(Uni<? extends T>... unis) {
        return all(asList(nonNull(unis, "unis")));
    }

    /**
     * Join multiple {@link Uni} references and emit a list of values.
     * <p>
     * The resulting list of values is in order of the list of {@link Uni}.
     * What happens when any of the {@link Uni} emits a failure rather than a value is specified by a subsequent call
     * to any of the methods in {@link JoinAllStrategy}.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, must not contain any {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @CheckReturnValue
    public final <T> JoinAllStrategy<T> all(List<Uni<? extends T>> unis) {
        return new JoinAllStrategy<>(doesNotContainNull(unis, "unis"));
    }

    /**
     * Defines how to deal with failures while joining {@link Uni} references with {@link UniJoin#all(List)}.
     *
     * @param <T> the type of the {@link Uni} values
     */
    public static class JoinAllStrategy<T> {

        private final List<Uni<? extends T>> unis;

        private JoinAllStrategy(List<Uni<? extends T>> unis) {
            this.unis = unis;
        }

        /**
         * Wait for all {@link Uni} references to terminate, and collect all failures in a
         * {@link io.smallrye.mutiny.CompositeException}.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        public Uni<List<T>> andCollectFailures() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.COLLECT_FAILURES));
        }

        /**
         * Immediately forward the first failure from any of the {@link Uni}, and cancel the remaining {@link Uni}
         * subscriptions, ignoring eventual subsequent failures.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        public Uni<List<T>> andFailFast() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.FAIL_FAST));
        }
    }

    /**
     * Join multiple {@link Uni} and emit the result from the first one to emit a signal, or the first one with a value.
     * This is a convenience delegate method for {@link #first(List)}.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, must not contain any {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> JoinFirstStrategy<T> first(Uni<? extends T>... unis) {
        return first(asList(nonNull(unis, "unis")));
    }

    /**
     * Join multiple {@link Uni} and emit the result from the first one to emit a signal, or the first one with a value.
     * <p>
     * What happens when any of the {@link Uni} emits a failure rather than a value is specified by a subsequent call
     * to any of the methods in {@link JoinFirstStrategy}.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, must not contain any {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @CheckReturnValue
    public final <T> JoinFirstStrategy<T> first(List<Uni<? extends T>> unis) {
        return new JoinFirstStrategy<>(doesNotContainNull(unis, "unis"));
    }

    /**
     * Defines how to deal with failures while joining {@link Uni} references with {@link UniJoin#first(List)}}.
     *
     * @param <T> the type of the {@link Uni} values
     */
    public static class JoinFirstStrategy<T> {

        private final List<Uni<? extends T>> unis;

        private JoinFirstStrategy(List<Uni<? extends T>> unis) {
            this.unis = unis;
        }

        /**
         * Forward the value or failure from the first {@link Uni} to terminate.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        public Uni<T> toTerminate() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_TO_EMIT));
        }

        /**
         * Forward the value from the first {@link Uni} to terminate with a value.
         * <p>
         * When all {@link Uni} references fail then failures are collected into a {@link CompositeException},
         * which is then forwarded by the returned {@link Uni}.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        public Uni<T> withItem() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_WITH_ITEM));
        }
    }

    /**
     * Provide a builder to assemble the {@link Uni} references to join.
     *
     * @param <T> the type of the {@link Uni} values
     * @return a new builder
     */
    @CheckReturnValue
    public <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder to assemble {@link Uni} references to be joined.
     *
     * @param <T> the type of the {@link Uni} values
     */
    public class Builder<T> {

        private final List<Uni<? extends T>> unis = new ArrayList<>();

        /**
         * Add a {@link Uni}.
         *
         * @param uni a {@link Uni}
         * @return this builder instance
         */
        @CheckReturnValue
        public Builder<T> add(Uni<? extends T> uni) {
            unis.add(uni);
            return this;
        }

        /**
         * Join all {@link Uni} references.
         *
         * @return the object to configure the failure management strategy
         */
        @CheckReturnValue
        public JoinAllStrategy<T> joinAll() {
            return UniJoin.this.all(unis);
        }

        /**
         * Join on one among the {@link Uni} references.
         *
         * @return the object to configure the failure management strategy
         */
        @CheckReturnValue
        public JoinFirstStrategy<T> joinFirst() {
            return UniJoin.this.first(unis);
        }
    }
}
