package io.smallrye.reactive.adapt;

import io.smallrye.reactive.Uni;

/**
 * An SPI to implement adaptation logic to and from {@link Uni}.
 * Each implementation provide a way to map an {@link Uni} into an instance of {@code O} and to map an instance of
 * {@code O} into an {@link Uni}.
 * <p>
 * For example, if you are a user of RX Java 2 and Reactor you would be able to write something like:
 * <code>
 * Maybe maybe = ...
 * Mono mono = Uni.from(maybe).to(Mono.class);
 * </code>
 *
 * @param <O> the supported type
 */
public interface UniAdapter<O> {

    /**
     * Method called to check if the current {@link UniAdapter} can handle the class {@code clazz}
     *
     * @param clazz the class, not {@code null}
     * @return {@code true} if this adapter can perform a conversion from/to this class.
     */
    boolean accept(Class<O> clazz);

    /**
     * Adapts an instance of {@link Uni} into an instance of the target class ({@code O}).
     *
     * @param uni the uni instance, not {@code null}
     * @return the created instance, must not be {@code null}
     */
    O adaptTo(Uni<?> uni);

    /**
     * Adapts an instance of {@code O} into a {@link Uni}.
     *
     * @param instance the instance of {@code O}, not {@code null}
     * @return the created {@link Uni}, must not be {@code null}
     */
    Uni<?> adaptFrom(O instance);

}
