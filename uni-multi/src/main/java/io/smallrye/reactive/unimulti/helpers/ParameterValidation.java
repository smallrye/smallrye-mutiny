package io.smallrye.reactive.unimulti.helpers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;

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
     * @param name the name of the parameter, must not be {@code null}
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
     * @param name the name of the parameter, must not be {@code null}
     * @param <T> the type of the instance
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
     * @param name the name of the parameter, must not be {@code null}
     * @return the amount is the validation passes.
     */
    public static long positive(long amount, String name) {
        nonNull(name, "name");
        if (amount <= 0) {
            throw new IllegalArgumentException(String.format("`%s` must be greater than zero`", name));
        }
        return amount;
    }

    /**
     * Validates that the passed amount is strictly positive.
     *
     * @param amount the amount to be checked
     * @param name the name of the parameter, must not be {@code null}
     * @return the amount is the validation passes.
     */
    public static int positive(int amount, String name) {
        nonNull(name, "name");
        if (amount <= 0) {
            throw new IllegalArgumentException(String.format("`%s` must be greater than zero", name));
        }
        return amount;
    }

    /**
     * Validates that the passed amount is positive (including 0).
     *
     * @param amount the amount to be checked
     * @param name the name of the parameter, must not be {@code null}
     * @return the amount is the validation passes.
     */
    public static int positiveOrZero(int amount, String name) {
        nonNull(name, "name");
        if (amount < 0) {
            throw new IllegalArgumentException(String.format("`%s` must be positive", name));
        }
        return amount;
    }

    /**
     * Ensures that the given iterable does not contain a {@code null} value.
     *
     * @param iterable the iterable
     * @param name the name of the parameter, must not be {@code null}
     * @param <T> the type of the instance
     * @return the instance if the validation passes
     */
    public static <T extends Iterable<?>> T doesNotContainNull(T iterable, String name) {
        nonNull(name, "name");
        nonNull(iterable, name);
        iterable.forEach(m -> {
            if (m == null) {
                throw new IllegalArgumentException(String.format("`%s` contains a `null` value", name));
            }
        });
        return iterable;
    }

    /**
     * Ensures that the given array does not contain a {@code null} value.
     *
     * @param array the array
     * @param name the name of the parameter, must not be {@code null}
     * @param <T> the type of the item contained in the array
     * @return the instance if the validation passes
     */
    public static <T> T[] doesNotContainNull(T[] array, String name) {
        nonNull(name, "name");
        nonNull(array, name);
        Arrays.stream(array).forEach(m -> {
            if (m == null) {
                throw new IllegalArgumentException(String.format("`%s` contains a `null` value", name));
            }
        });
        return array;
    }

    /**
     * Validates that the given collection {@code instance} has size matching the {@code expectedSize}
     *
     * @param instance the instance
     * @param expectedSize the expected size
     * @param name the name of the parameter, must not be {@code null}
     * @param <T> the type of the instance
     * @return the instance if the validation passes
     */
    public static <T extends Collection<?>> T size(T instance, int expectedSize, String name) {
        if (name == null) {
            throw new IllegalArgumentException("The parameter name must be set");
        }
        if (instance == null) {
            throw new IllegalArgumentException(String.format("`%s` must not be `null`", name));
        }
        if (instance.size() != expectedSize) {
            throw new IllegalArgumentException(String.format("`%s` must has size %d, but was %d", name, expectedSize,
                    instance.size()));
        }
        return instance;
    }
}
