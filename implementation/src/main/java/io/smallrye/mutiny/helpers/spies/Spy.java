package io.smallrye.mutiny.helpers.spies;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Function;
import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiOverflowStrategy;

/**
 * Helpers for creating {@link Uni} and {@link Multi} spies to observe events.
 * <p>
 * A spy is a transparent operator that can be plugged into a pipeline to help diagnose what events flow.
 * It observes how many times a given event has been observed.
 * Depending on the event type it will also report values, such as the last observed failure or termination.
 * <p>
 * It is important to note that spies observe and report events for all subscribers, not just one in particular.
 */
public interface Spy {

    // --------------------------------------------------------------------- //

    /**
     * Spy {@link Uni#onSubscription()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnSubscribeSpy<T> onSubscribe(Uni<T> upstream) {
        return (UniOnSubscribeSpy<T>) upstream.plug(uni -> new UniOnSubscribeSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onCancellation()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnCancellationSpy<T> onCancellation(Uni<T> upstream) {
        return (UniOnCancellationSpy<T>) upstream.plug(uni -> new UniOnCancellationSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onTermination()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnTerminationSpy<T> onTermination(Uni<T> upstream) {
        return (UniOnTerminationSpy<T>) upstream.plug(uni -> new UniOnTerminationSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onItem()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnItemSpy<T> onItem(Uni<T> upstream) {
        return (UniOnItemSpy<T>) upstream.plug(uni -> new UniOnItemSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onItemOrFailure()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnItemOrFailureSpy<T> onItemOrFailure(Uni<T> upstream) {
        return (UniOnItemOrFailureSpy<T>) upstream.plug(uni -> new UniOnItemOrFailureSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onFailure()} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream));
    }

    /**
     * Spy {@link Uni#onFailure(Predicate)} )} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @param predicate a predicate to match a failure type
     * @return a new {@link Uni}
     */
    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream, predicate));
    }

    /**
     * Spy {@link Uni#onFailure(Class)} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @param typeOfFailure the expected failure type
     * @return a new {@link Uni}
     */
    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream, Class<? extends Throwable> typeOfFailure) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream, typeOfFailure));
    }

    /**
     * Spy all {@link Uni} events.
     *
     * @param upstream the upstream
     * @param <T> the item type
     * @return a new {@link Uni}
     */
    static <T> UniGlobalSpy<T> globally(Uni<T> upstream) {
        return (UniGlobalSpy<T>) upstream.plug(uni -> new UniGlobalSpy<>(upstream));
    }

    // --------------------------------------------------------------------- //

    /**
     * Spy {@link Multi#onCancellation()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnCancellationSpy<T> onCancellation(Multi<T> upstream) {
        return (MultiOnCancellationSpy<T>) upstream.plug(multi -> new MultiOnCancellationSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onCompletion()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnCompletionSpy<T> onCompletion(Multi<T> upstream) {
        return (MultiOnCompletionSpy<T>) upstream.plug(multi -> new MultiOnCompletionSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onFailure()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onFailure(Predicate)} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @param predicate a predicate to match a failure type
     * @return a new {@link Multi}
     */
    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream, predicate));
    }

    /**
     * Spy {@link Multi#onFailure(Class)} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @param typeOfFailure the type of failure
     * @return a new {@link Multi}
     */
    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream, Class<? extends Throwable> typeOfFailure) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream, typeOfFailure));
    }

    /**
     * Spy {@link Multi#onItem()} events and keep track of the items.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnItemSpy<T> onItem(Multi<T> upstream) {
        return onItem(upstream, true);
    }

    /**
     * Spy {@link Multi#onItem()} events and optionally keep track of the items.
     *
     * @param upstream the upstream
     * @param trackItems {@code true} when items shall be tracked, {@code false} otherwise
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnItemSpy<T> onItem(Multi<T> upstream, boolean trackItems) {
        return (MultiOnItemSpy<T>) upstream.plug(multi -> new MultiOnItemSpy<>(upstream, trackItems));
    }

    /**
     * Spy {@link Multi#onRequest()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnRequestSpy<T> onRequest(Multi<T> upstream) {
        return (MultiOnRequestSpy<T>) upstream.plug(multi -> new MultiOnRequestSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onSubscription()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnSubscribeSpy<T> onSubscribe(Multi<T> upstream) {
        return (MultiOnSubscribeSpy<T>) upstream.plug(multi -> new MultiOnSubscribeSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onTermination()} events.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnTerminationSpy<T> onTermination(Multi<T> upstream) {
        return (MultiOnTerminationSpy<T>) upstream.plug(multi -> new MultiOnTerminationSpy<>(upstream));
    }

    /**
     * Spy {@link Multi#onOverflow()} events and track dropped items.
     *
     * @param upstream the uptream
     * @param strategyMapper a function to define the drop strategy applied by the spy
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnOverflowSpy<T> onOverflow(Multi<T> upstream,
            Function<MultiOverflowStrategy<? extends T>, Multi<? extends T>> strategyMapper) {
        return onOverflow(upstream, true, strategyMapper);
    }

    /**
     * Spy {@link Multi#onOverflow()} events and track dropped items.
     *
     * @param upstream the uptream
     * @param trackItems {@code true} if items shall be tracked, {@code false} otherwise
     * @param strategyMapper a function to define the drop strategy applied by the spy
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiOnOverflowSpy<T> onOverflow(Multi<T> upstream, boolean trackItems,
            Function<MultiOverflowStrategy<? extends T>, Multi<? extends T>> strategyMapper) {
        return (MultiOnOverflowSpy<T>) upstream.plug(multi -> {
            Function<MultiOverflowStrategy<? extends T>, Multi<? extends T>> actual = nonNull(strategyMapper, "strategyMapper");
            return new MultiOnOverflowSpy<>(upstream, trackItems, actual);
        });
    }

    /**
     * Spy all {@link Multi} events (except {@link Multi#onOverflow()}).
     * <p>
     * The overflow events are not being tracked because they cause the {@code upstream} to be requested {@link Long#MAX_VALUE}
     * elements, so the behavior of a pipeline can be affected by introducing a {@link MultiOnOverflowSpy}.
     * <p>
     * If you want to track overflow events then you will need to explicitly wrap a {@link Multi} with
     * {@link Spy#onOverflow(Multi, Function)}.
     *
     * @param upstream the upstream
     * @param <T> the items type
     * @return a new {@link Multi}
     */
    static <T> MultiGlobalSpy<T> globally(Multi<T> upstream) {
        return (MultiGlobalSpy<T>) upstream.plug(multi -> new MultiGlobalSpy<>(upstream));
    }
}
