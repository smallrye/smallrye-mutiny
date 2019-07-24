package io.smallrye.reactive.helpers;

import java.time.Duration;

/**
 * A class to validate method parameters.
 * these methods throw {@link IllegalArgumentException} is the validation fails.
 */
public class ParameterValidation {

    public static final String SUPPLIER_PRODUCED_NULL = "The supplier returned `null`";
    public static final String MAPPER_RETURNED_NULL = "The mapper returned `null`";


    private ParameterValidation() {
        // avoid direct instantiation
    }

    /**
     * Validates that the passed duration is not {@code null} and strictly positive.
     *
     * @param duration the duration
     * @param name     the name of the parameter, must not be {@code null}
     * @return the duration is the validation passes.
     */
    public static Duration validate(Duration duration, String name) {
        nonNull(name, "name");
        if (duration == null) {
            throw new IllegalArgumentException(String.format("`%s` must not be `null`", name));
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(String.format("`%s` must be greater than zero`", name));
        }
        return duration;
    }

    /**
     * Validates that the given {@code instance} is not {@code null}
     *
     * @param instance the instance
     * @param name     the name of the parameter, must not be {@code null}
     * @param <T>      the type of the instance
     * @return the instance if the validation passes
     */
    public static <T> T nonNull(T instance, String name) {
        if (name == null) {
            throw new IllegalArgumentException("The parameter name must be set");
        }
        if (instance == null) {
            throw new IllegalArgumentException(String.format("`%s` must not be `null`", name));
        }
        return instance;
    }

    /**
     * Validates that the passed amount is strictly positive.
     *
     * @param amount the amount to be checked
     * @param name     the name of the parameter, must not be {@code null}
     * @return the amount is the validation passes.
     */
    public static long positive(long amount, String name) {
        nonNull(name, "name");
        if (amount <= 0) {
            throw new IllegalArgumentException(String.format("`%s` must be greater than zero`", name));
        }
        return amount;
    }

}
