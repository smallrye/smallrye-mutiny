package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Arrays;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.*;

/**
 * Combines several {@link Uni unis} into a new {@link Uni} that will be fulfilled when <strong>all</strong>
 * {@link Uni unis} have emitted an {@code item} event and then combines the different outcomes into a
 * {@link Tuple}, or using a combinator function.
 * <p>
 * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
 * causes the other {@link Uni unis} to be cancelled, expect if {@code collectFailures()} is invoked, which delay the
 * {@code failure} event until all {@link Uni}s have completed or failed.
 */
public class UniAndGroup<T1> {

    private final Uni<T1> upstream;

    public UniAndGroup(Uni<T1> upstream) {
        this.upstream = nonNull(upstream, "upstream");
    }

    /**
     * Combines the current {@link Uni} with the given one.
     * Once both {@link Uni} have completed successfully, the item can be retrieved as a
     * {@link Tuple2} or computed using a {@link java.util.function.BiFunction}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup2#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param other the other uni, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @return an {@link UniAndGroup2} to configure the combination
     */
    @CheckReturnValue
    public <T2> UniAndGroup2<T1, T2> uni(Uni<? extends T2> other) {
        return new UniAndGroup2<>(upstream, other);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple3} or computed
     * using a {@link Functions.Function3}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup3#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @return an {@link UniAndGroup3} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3> UniAndGroup3<T1, T2, T3> unis(Uni<? extends T2> u2, Uni<? extends T3> u3) {
        return new UniAndGroup3<>(upstream, u2, u3);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple4} or computed
     * using a {@link Functions.Function4}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup4#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @return an {@link UniAndGroup4} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4> UniAndGroup4<T1, T2, T3, T4> unis(Uni<? extends T2> u2, Uni<? extends T3> u3,
            Uni<? extends T4> u4) {
        return new UniAndGroup4<>(upstream, u2, u3, u4);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple5} or computed
     * using a {@link Functions.Function5}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup5#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param u5 the fifth uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @param <T5> the type of the item for the fifth uni
     * @return an {@link UniAndGroup5} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4, T5> UniAndGroup5<T1, T2, T3, T4, T5> unis(Uni<? extends T2> u2, Uni<? extends T3> u3,
            Uni<? extends T4> u4, Uni<? extends T5> u5) {
        return new UniAndGroup5<>(upstream, u2, u3, u4, u5);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple6} or computed
     * using a {@link Functions.Function5}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup6#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param u5 the fifth uni to be combined, must not be {@code null}
     * @param u6 the sixth uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @param <T5> the type of the item for the fifth uni
     * @param <T6> the type of the item for the sixth uni
     * @return an {@link UniAndGroup6} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4, T5, T6> UniAndGroup6<T1, T2, T3, T4, T5, T6> unis(Uni<? extends T2> u2, Uni<? extends T3> u3,
            Uni<? extends T4> u4, Uni<? extends T5> u5, Uni<? extends T6> u6) {
        return new UniAndGroup6<>(upstream, u2, u3, u4, u5, u6);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple7} or computed
     * using a {@link Functions.Function7}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup7#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param u5 the fifth uni to be combined, must not be {@code null}
     * @param u6 the sixth uni to be combined, must not be {@code null}
     * @param u7 the seventh uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @param <T5> the type of the item for the fifth uni
     * @param <T6> the type of the item for the sixth uni
     * @param <T7> the type of the item for the seventh uni
     * @return an {@link UniAndGroup7} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4, T5, T6, T7> UniAndGroup7<T1, T2, T3, T4, T5, T6, T7> unis( // NOSONAR
            Uni<? extends T2> u2, Uni<? extends T3> u3, Uni<? extends T4> u4, Uni<? extends T5> u5,
            Uni<? extends T6> u6, Uni<? extends T7> u7) {
        return new UniAndGroup7<>(upstream, u2, u3, u4, u5, u6, u7);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple8} or computed
     * using a {@link Functions.Function8}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup8#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param u5 the fifth uni to be combined, must not be {@code null}
     * @param u6 the sixth uni to be combined, must not be {@code null}
     * @param u7 the seventh uni to be combined, must not be {@code null}
     * @param u8 the eighth uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @param <T5> the type of the item for the fifth uni
     * @param <T6> the type of the item for the sixth uni
     * @param <T7> the type of the item for the seventh uni
     * @param <T8> the type of the item for the eighth uni
     * @return an {@link UniAndGroup8} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4, T5, T6, T7, T8> UniAndGroup8<T1, T2, T3, T4, T5, T6, T7, T8> unis( // NOSONAR
            Uni<? extends T2> u2, Uni<? extends T3> u3, Uni<? extends T4> u4, Uni<? extends T5> u5,
            Uni<? extends T6> u6, Uni<? extends T7> u7, Uni<? extends T8> u8) {
        return new UniAndGroup8<>(upstream, u2, u3, u4, u5, u6, u7, u8);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item can be retrieved as a {@link Tuple9} or computed
     * using a {@link Functions.Function9}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup9#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u2 the second uni to be combined, must not be {@code null}
     * @param u3 the third uni to be combined, must not be {@code null}
     * @param u4 the fourth uni to be combined, must not be {@code null}
     * @param u5 the fifth uni to be combined, must not be {@code null}
     * @param u6 the sixth uni to be combined, must not be {@code null}
     * @param u7 the seventh uni to be combined, must not be {@code null}
     * @param u8 the eighth uni to be combined, must not be {@code null}
     * @param u9 the ninth uni to be combined, must not be {@code null}
     * @param <T2> the type of the item for the second uni
     * @param <T3> the type of the item for the third uni
     * @param <T4> the type of the item for the fourth uni
     * @param <T5> the type of the item for the fifth uni
     * @param <T6> the type of the item for the sixth uni
     * @param <T7> the type of the item for the seventh uni
     * @param <T8> the type of the item for the eighth uni
     * @param <T9> the type of the item for the ninth uni
     * @return an {@link UniAndGroup9} to configure the combination
     */
    @CheckReturnValue
    public <T2, T3, T4, T5, T6, T7, T8, T9> UniAndGroup9<T1, T2, T3, T4, T5, T6, T7, T8, T9> unis( // NOSONAR
            Uni<? extends T2> u2, Uni<? extends T3> u3, Uni<? extends T4> u4, Uni<? extends T5> u5,
            Uni<? extends T6> u6, Uni<? extends T7> u7, Uni<? extends T8> u8, Uni<? extends T9> u9) {
        return new UniAndGroup9<>(upstream, u2, u3, u4, u5, u6, u7, u8, u9);
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item is computed using a {@code combinator} function
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroupIterable#collectFailures()} is
     * invoked which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param unis the list of unis, must not be {@code null}, must not contain {@code null}, must not be empty
     * @return an {@link UniAndGroupIterable} to configure the combination
     */
    @CheckReturnValue
    public UniAndGroupIterable<T1> unis(Uni<?>... unis) {
        return unis(Arrays.asList(nonNull(unis, "unis")));
    }

    /**
     * Combines the current {@link Uni} with the given ones.
     * Once all {@link Uni} have completed successfully, the item is computed using a {@code combinator} function
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroupIterable#collectFailures()} is
     * invoked which delay the failure event until all {@link Uni}s have fires an item or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param unis the list of unis, must not be {@code null}, must not contain {@code null}, must not be empty
     * @return an {@link UniAndGroupIterable} to configure the combination
     */
    @CheckReturnValue
    public UniAndGroupIterable<T1> unis(Iterable<? extends Uni<?>> unis) {
        return new UniAndGroupIterable<>(upstream, unis);
    }

}
