package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.*;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.uni.builders.UniJoinAll;
import io.smallrye.mutiny.operators.uni.builders.UniJoinFirst;

/**
 * Join multiple {@link Uni Unis}.
 * <p>
 * <strong>Note about emptiness:</strong> If the set of Unis is empty, the set is rejected. Joining an empty set would
 * not propagate any event as it would not subscribe to anything. As a result, you cannot join empty sets of Unis. An
 * {@link IllegalArgumentException} will be thrown in this case.
 * </p>
 */
public class UniJoin {

    public static final UniJoin SHARED_INSTANCE = new UniJoin();

    private UniJoin() {
        // Do nothing
    }

    /**
     * Join multiple {@link Uni} references and emit a list of values, this is a convenience delegate method for
     * {@link #all(List)}.
     * <p>
     * The list of {@code Unis} must not be {@code null}, empty, or contain {@code null} objects.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null} or empty, must not contain any {@code null}
     *        reference.
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> JoinAllStrategy<T> all(Uni<T>... unis) {
        return all(asList(nonNull(unis, "unis")));
    }

    /**
     * Join multiple {@link Uni} references and emit a list of values.
     * <p>
     * The resulting list of values is in order of the list of {@link Uni}.
     * What happens when any of the {@link Uni} emits a failure rather than a value is specified by a subsequent call
     * to any of the methods in {@link JoinAllStrategy}.
     * <p>
     * The list of {@code Unis} must not be {@code null}, empty, or contain {@code null} objects.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, must not contain any {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @CheckReturnValue
    public final <T> JoinAllStrategy<T> all(List<Uni<T>> unis) {
        doesNotContainNull(unis, "unis");
        isNotEmpty(unis, "unis");
        return new JoinAllStrategy<>(unis);
    }

    /**
     * Terminal interface for {@link UniJoin#all(List)}
     *
     * @param <T> the type of the {@link Uni} values
     */
    public interface JoinAllStrategyTerminal<T> {

        /**
         * Wait for all {@link Uni} references to terminate, and collect all failures in a
         * {@link io.smallrye.mutiny.CompositeException}.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        Uni<List<T>> andCollectFailures();

        /**
         * Immediately forward the first failure from any of the {@link Uni}, and cancel the remaining {@link Uni}
         * subscriptions, ignoring eventual subsequent failures.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        Uni<List<T>> andFailFast();
    }

    /**
     * Defines how to deal with failures while joining {@link Uni} references with {@link UniJoin#all(List)}.
     *
     * @param <T> the type of the {@link Uni} values
     */
    public static class JoinAllStrategy<T> implements JoinAllStrategyTerminal<T> {

        private final List<Uni<T>> unis;
        private int concurrency = -1;

        private JoinAllStrategy(List<Uni<T>> unis) {
            this.unis = unis;
        }

        /**
         * Limit the number of concurrent upstream subscriptions.
         * <p>
         * When not specified all upstream {@link Uni} are being subscribed when the joining {@link Uni} is subscribed.
         * <p>
         * Setting a limit is useful when you have a large number of {@link Uni} to join and their simultaneous
         * subscriptions might overwhelm resources (e.g., database connections, etc).
         *
         * @param limit the concurrency limit, must be strictly positive
         * @return an object to conclude the join strategy
         */
        @CheckReturnValue
        public JoinAllStrategyTerminal<T> usingConcurrencyOf(int limit) {
            this.concurrency = ParameterValidation.positive(limit, "limit");
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @CheckReturnValue
        public Uni<List<T>> andCollectFailures() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.COLLECT_FAILURES, concurrency));
        }

        /**
         * {@inheritDoc}
         */
        @CheckReturnValue
        public Uni<List<T>> andFailFast() {
            return Infrastructure.onUniCreation(new UniJoinAll<>(unis, UniJoinAll.Mode.FAIL_FAST, concurrency));
        }
    }

    /**
     * Join multiple {@link Uni} and emit the result from the first one to emit a signal, or the first one with a value.
     * This is a convenience delegate method for {@link #first(List)}.
     * <p>
     * The list of {@code Unis} must not be {@code null}, empty, or contain {@code null} objects.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, empty and must not contain any
     *        {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @SafeVarargs
    @CheckReturnValue
    public final <T> JoinFirstStrategy<T> first(Uni<T>... unis) {
        return first(asList(nonNull(unis, "unis")));
    }

    /**
     * Join multiple {@link Uni} and emit the result from the first one to emit a signal, or the first one with a value.
     * <p>
     * What happens when any of the {@link Uni} emits a failure rather than a value is specified by a subsequent call
     * to any of the methods in {@link JoinFirstStrategy}.
     * <p>
     * The list of {@code Unis} must not be {@code null}, empty, or contain {@code null} objects.
     *
     * @param unis the list of {@link Uni} to join, must not be {@code null}, empty, must not contain any
     *        {@code null} reference
     * @param <T> the type of the {@link Uni} values
     * @return the object to configure the failure management strategy
     */
    @CheckReturnValue
    public final <T> JoinFirstStrategy<T> first(List<Uni<T>> unis) {
        doesNotContainNull(unis, "unis");
        isNotEmpty(unis, "unis");
        return new JoinFirstStrategy<>(unis);
    }

    /**
     * Terminal interface for {@link UniJoin#first(List)}
     *
     * @param <T> the type of the {@link Uni} values
     */
    public interface JoinFirstStrategyTerminal<T> {

        /**
         * Forward the value from the first {@link Uni} to terminate with a value.
         * <p>
         * When all {@link Uni} references fail then failures are collected into a {@link CompositeException},
         * which is then forwarded by the returned {@link Uni}.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        Uni<T> withItem();
    }

    /**
     * Defines how to deal with failures while joining {@link Uni} references with {@link UniJoin#first(List)}}.
     *
     * @param <T> the type of the {@link Uni} values
     */
    public static class JoinFirstStrategy<T> implements JoinFirstStrategyTerminal<T> {

        private final List<Uni<T>> unis;
        private int concurrency = -1;

        private JoinFirstStrategy(List<Uni<T>> unis) {
            this.unis = unis;
        }

        /**
         * Limit the number of concurrent upstream subscriptions.
         * <p>
         * When not specified all upstream {@link Uni} are being subscribed when the joining {@link Uni} is subscribed.
         * <p>
         * Setting a limit is useful when you have a large number of {@link Uni} to join and their simultaneous
         * subscriptions might overwhelm resources (e.g., database connections, etc).
         *
         * @param limit the concurrency limit, must be strictly positive
         * @return an object to conclude the join strategy
         */
        @CheckReturnValue
        public JoinFirstStrategyTerminal<T> usingConcurrencyOf(int limit) {
            this.concurrency = ParameterValidation.positive(limit, "limit");
            return this;
        }

        /**
         * Forward the value or failure from the first {@link Uni} to terminate.
         *
         * @return a new {@link Uni}
         */
        @CheckReturnValue
        public Uni<T> toTerminate() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_TO_EMIT, concurrency));
        }

        /**
         * {@inheritDoc}
         */
        @CheckReturnValue
        public Uni<T> withItem() {
            return Infrastructure.onUniCreation(new UniJoinFirst<>(unis, UniJoinFirst.Mode.FIRST_WITH_ITEM, concurrency));
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

        private final List<Uni<T>> unis = new ArrayList<>();

        /**
         * Add a {@link Uni}.
         *
         * @param uni a {@link Uni}
         * @return this builder instance
         */
        @CheckReturnValue
        public Builder<T> add(Uni<T> uni) {
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
