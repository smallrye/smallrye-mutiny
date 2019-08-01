package io.smallrye.reactive.groups;


import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.tuples.*;

import java.util.Arrays;

/**
 * Combines several {@link Uni} into a new {@link Uni} that will fire a result event when all {@link Uni} are
 * resolved successfully aggregating their results into a {@link Tuple}, or using a combinator function.
 * <p>
 * The produced {@link Uni} fire a failure if one of {@link Uni Unis} produces a failure. This will
 * cause the other {@link Uni} to be cancelled, expect if {@code collectFailures()} is invoked, which delay firing
 * the failure until all {@link Uni}s have completed or failed.
 */
public class UniZip {

    public static final UniZip INSTANCE = new UniZip();


    private UniZip() {
        // avoid direct instantiation
    }

    /**
     * Combines two {@link Uni unis} together.
     * Once both {@link Uni} have completed successfully, the result can be retrieved as a
     * {@link Pair} or computed using a {@link java.util.function.BiFunction}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup2#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u1   the first uni, must not be {@code null}
     * @param u2   the second uni, must not be {@code null}
     * @param <T1> the type of the result for the first uni
     * @param <T2> the type of the result for the second uni
     * @return an {@link UniAndGroup2} to configure the combination
     */
    public <T1, T2> UniAndGroup2<T1, T2> unis(Uni<? extends T1> u1, Uni<? extends T2> u2) {
        return new UniAndGroup2<>(u1, u2);
    }

    /**
     * Combines the three {@link Uni unis} together.§
     * Once all {@link Uni} have completed successfully, the result can be retrieved as a {@link Tuple3} or computed
     * using a {@link Functions.Function3}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup3#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u1   the first uni to be combined, must not be {@code null}
     * @param u2   the second uni to be combined, must not be {@code null}
     * @param u3   the third uni to be combined, must not be {@code null}
     * @param <T1> the type of the result for the first uni
     * @param <T2> the type of the result for the second uni
     * @param <T3> the type of the result for the third uni
     * @return an {@link UniAndGroup3} to configure the combination
     */
    public <T1, T2, T3> UniAndGroup3<T1, T2, T3> unis(Uni<? extends T1> u1, Uni<? extends T2> u2, Uni<? extends T3> u3) {
        return new UniAndGroup3<>(u1, u2, u3);
    }

    /**
     * Combines four {@link Uni unis} together.
     * Once all {@link Uni} have completed successfully, the result can be retrieved as a {@link Tuple4} or computed
     * using a {@link Functions.Function4}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup4#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u1   the first uni to be combined, must not be {@code null}
     * @param u2   the second uni to be combined, must not be {@code null}
     * @param u3   the third uni to be combined, must not be {@code null}
     * @param u4   the fourth uni to be combined, must not be {@code null}
     * @param <T1> the type of the result for the first uni
     * @param <T2> the type of the result for the second uni
     * @param <T3> the type of the result for the third uni
     * @param <T4> the type of the result for the fourth uni
     * @return an {@link UniAndGroup4} to configure the combination
     */
    public <T1, T2, T3, T4> UniAndGroup4<T1, T2, T3, T4> unis(Uni<? extends T1> u1, Uni<? extends T2> u2,
                                                              Uni<? extends T3> u3, Uni<? extends T4> u4) {
        return new UniAndGroup4<>(u1, u2, u3, u4);
    }

    /**
     * Combines five {@link Uni unis} together.
     * Once all {@link Uni} have completed successfully, the result can be retrieved as a {@link Tuple4} or computed
     * using a {@link Functions.Function4}.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroup5#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param u1   the first uni to be combined, must not be {@code null}
     * @param u2   the second uni to be combined, must not be {@code null}
     * @param u3   the third uni to be combined, must not be {@code null}
     * @param u4   the fourth uni to be combined, must not be {@code null}
     * @param u5   the fifth uni to be combined, must not be {@code null}
     * @param <T1> the type of the result for the first uni
     * @param <T2> the type of the result for the second uni
     * @param <T3> the type of the result for the third uni
     * @param <T4> the type of the result for the fourth uni
     * @param <T5> the type of the result for the fifth uni
     * @return an {@link UniAndGroup5} to configure the combination
     */
    public <T1, T2, T3, T4, T5> UniAndGroup5<T1, T2, T3, T4, T5> unis(Uni<? extends T1> u1, Uni<? extends T2> u2,
                                                                      Uni<? extends T3> u3, Uni<? extends T4> u4,
                                                                      Uni<? extends T5> u5) {
        return new UniAndGroup5<>(u1, u2, u3, u4, u5);
    }

    /**
     * Combines several {@link Uni unis} together.
     * Once all {@link Uni} have completed successfully, the result is computed using a {@code combinator} function.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroupIterable#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param unis the list of unis, must not be {@code null}, must not contain {@code null}, must not be empty
     * @return an {@link UniAndGroupIterable} to configure the combination
     */
    public UniAndGroupIterable unis(Uni<?>... unis) {
        return unis(Arrays.asList(unis));
    }

    /**
     * Combines several {@link Uni unis} together.
     * Once all {@link Uni} have completed successfully, the result is computed using a {@code combinator} function.
     * <p>
     * The produced {@link Uni} fires a {@code failure} event if one of the {@link Uni Unis} fires a failure. This
     * will cause the other {@link Uni} to be cancelled, expect if {@link UniAndGroupIterable#collectFailures()} is invoked
     * which delay the failure event until all {@link Uni}s have fires a result or failure event. If several
     * {@link Uni unis} fire a failure, the propagated failure is a {@link CompositeException} wrapping all the
     * collected failures.
     *
     * @param unis the list of unis, must not be {@code null}, must not contain {@code null}, must not be empty
     * @return an {@link UniAndGroupIterable} to configure the combination
     */
    public UniAndGroupIterable unis(Iterable<? extends Uni<?>> unis) {
        return new UniAndGroupIterable<>(unis);
    }


}
