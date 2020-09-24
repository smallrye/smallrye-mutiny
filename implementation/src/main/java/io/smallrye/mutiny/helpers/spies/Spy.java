package io.smallrye.mutiny.helpers.spies;

import java.util.function.Predicate;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.MultiOnFailureInvoke;

public interface Spy {

    // --------------------------------------------------------------------- //

    static <T> UniOnSubscribeSpy<T> onSubscribe(Uni<T> upstream) {
        return (UniOnSubscribeSpy<T>) upstream.plug(uni -> new UniOnSubscribeSpy<>(upstream));
    }

    static <T> UniOnCancellationSpy<T> onCancellation(Uni<T> upstream) {
        return (UniOnCancellationSpy<T>) upstream.plug(uni -> new UniOnCancellationSpy<>(upstream));
    }

    static <T> UniOnTerminationSpy<T> onTermination(Uni<T> upstream) {
        return (UniOnTerminationSpy<T>) upstream.plug(uni -> new UniOnTerminationSpy<>(upstream));
    }

    static <T> UniOnItemSpy<T> onItem(Uni<T> upstream) {
        return (UniOnItemSpy<T>) upstream.plug(uni -> new UniOnItemSpy<>(upstream));
    }

    static <T> UniOnItemOrFailureSpy<T> onItemOrFailure(Uni<T> upstream) {
        return (UniOnItemOrFailureSpy<T>) upstream.plug(uni -> new UniOnItemOrFailureSpy<>(upstream));
    }

    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream));
    }

    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream, Predicate<? super Throwable> predicate) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream, predicate));
    }

    static <T> UniOnFailureSpy<T> onFailure(Uni<T> upstream, Class<? extends Throwable> typeOfFailure) {
        return (UniOnFailureSpy<T>) upstream.plug(uni -> new UniOnFailureSpy<>(upstream, typeOfFailure));
    }

    // --------------------------------------------------------------------- //

    static <T> MultiOnCancellationSpy<T> onCancellation(Multi<T> upstream) {
        return (MultiOnCancellationSpy<T>) upstream.plug(multi -> new MultiOnCancellationSpy<>(upstream));
    }

    static <T> MultiOnCompletionSpy<T> onCompletion(Multi<T> upstream) {
        return (MultiOnCompletionSpy<T>) upstream.plug(multi -> new MultiOnCompletionSpy<>(upstream));
    }

    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream));
    }

    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream, Predicate<? super Throwable> predicate) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream, predicate));
    }

    static <T> MultiOnFailureSpy<T> onFailure(Multi<T> upstream, Class<? extends Throwable> typeOfFailure) {
        return (MultiOnFailureSpy<T>) upstream.plug(multi -> new MultiOnFailureSpy<>(upstream, typeOfFailure));
    }
}
