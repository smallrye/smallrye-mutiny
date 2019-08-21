package io.smallrye.reactive.groups;


import io.smallrye.reactive.Uni;
import io.smallrye.reactive.operators.UniOrCombination;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;


public class UniAny {

    public static final UniAny INSTANCE = new UniAny();

    private UniAny() {
        // avoid direct instantiation.
    }


    /**
     * Like {@link #of(Iterable)} but with an array of {@link Uni} as parameter
     *
     * @param unis the array, must not be {@code null}, must not contain @{code null}
     * @param <T>  the type of item emitted by the different unis.
     * @return the produced {@link Uni}
     */
    @SafeVarargs
    public final <T> Uni<T> of(Uni<? super T>... unis) {
        List<Uni<? super T>> list = Arrays.asList(nonNull(unis, "unis"));
        return new UniOrCombination<>(list);
    }

    /**
     * Creates a {@link Uni} forwarding the first event (item or failure). It behaves like the fastest
     * of these competing unis. If the passed iterable is empty, the resulting {@link Uni} gets a {@code null} item
     * just after subscription.
     * <p>
     * This method subscribes to the set of {@link Uni}. When one of the {@link Uni} fires an item or a failure
     * a failure, the event is propagated downstream. Also the other subscriptions are cancelled.
     * <p>
     * Note that the callback from the subscriber are called on the thread used to fire the event of the selected
     * {@link Uni}. Use {@link Uni#receiveItemOn(Executor)} to change that thread.
     * <p>
     * If the subscription to the returned {@link Uni} is cancelled, the subscription to the {@link Uni unis}
     * contained in the {@code iterable} are also cancelled.
     *
     * @param iterable a set of {@link Uni}, must not be {@code null}.
     * @param <T>      the type of item emitted by the different unis.
     * @return the produced {@link Uni}
     */
    public <T> Uni<T> of(Iterable<? extends Uni<? super T>> iterable) {
        return new UniOrCombination<>(iterable);
    }
}
