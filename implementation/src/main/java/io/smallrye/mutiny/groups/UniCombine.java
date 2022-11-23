package io.smallrye.mutiny.groups;

import java.util.concurrent.Executor;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple;
import io.smallrye.mutiny.tuples.Tuple2;
import io.smallrye.mutiny.tuples.Tuple5;

public class UniCombine {

    public static final UniCombine INSTANCE = new UniCombine();

    private UniCombine() {
        // avoid direct instantiation
    }

    /**
     * Creates a {@link Uni} forwarding the first event (item or failure). It behaves like the fastest
     * of these competing unis. If the passed iterable is empty, the resulting {@link Uni} gets a {@code null} item
     * just after subscription.
     * <p>
     * This method subscribes to the set of {@link Uni}. When one of the {@link Uni} fires an item or a failure,
     * the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the event of the selected
     * {@link Uni}. Use {@link Uni#emitOn(Executor)} to change that thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis}
     * contained in the {@code iterable} are also cancelled.
     *
     * @return the object to enlist the candidates
     */
    @CheckReturnValue
    public UniAny any() {
        return UniAny.INSTANCE;
    }

    /**
     * Combines a set of {@link Uni unis} into a joined item. This item can be a {@code Tuple} or the item of a
     * combinator function.
     * <p>
     * If one of the combine {@link Uni} fire a failure, the other unis are cancelled, and the resulting
     * {@link Uni} fires the failure. If {@code collectFailures()} is called,
     * it waits for the completion of all the {@link Uni unis} before propagating the failure event. If more than one
     * {@link Uni} failed, a {@link CompositeException} is fired, wrapping the different collected failures.
     * <p>
     * Depending on the number of participants, the produced {@link Tuple} is
     * different from {@link Tuple2} to {@link Tuple5}. For more participants,
     * use {@link UniZip#unis(Uni[])} or
     * {@link UniZip#unis(Iterable)}.
     *
     * @return the object to configure the join
     */
    @CheckReturnValue
    public UniZip all() {
        return UniZip.INSTANCE;
    }

}
