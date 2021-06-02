package io.smallrye.mutiny.math;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

/**
 * Operator computing statistics based on the items emitted by the upstreams.
 * For each items from upstream, it structure containing the:
 * <ul>
 * <li>Count (number of items emitted by the upstream)</li>
 * <li>Average (Mean)</li>
 * <li>Variance</li>
 * <li>Standard Deviation</li>
 * <li>Skewness</li>
 * <li>Kurtosis</li>
 * <li>Min</li>
 * <li>Max</li>
 * </ul>
 * <p>
 * Computation following https://www.johndcook.com/blog/skewness_kurtosis/.
 */
public class StatisticsOperator<T extends Number & Comparable<T>> implements Function<Multi<T>, Multi<Statistic<T>>> {

    private final Statistic<T> EMPTY = new Statistic<>(0L, 0.0d, 0.0d, 0.0d, 0.0d, null, null);
    private final AtomicReference<Statistic<T>> currentStatistic = new AtomicReference<>(EMPTY);

    @Override
    public Multi<Statistic<T>> apply(Multi<T> multi) {
        return multi
                .onItem().transform(this::push)
                .onCompletion().ifEmpty().continueWith(EMPTY);
    }

    private Statistic<T> push(T number) {
        return currentStatistic.updateAndGet(stat -> {
            double delta, delta_n, delta_n2, term1;
            long n1 = stat.n;
            long n = stat.n + 1;
            double m1 = stat.m1;
            double m2 = stat.m2;
            double m3 = stat.m3;
            double m4 = stat.m4;
            T min = stat.min;
            T max = stat.max;

            delta = number.doubleValue() - stat.m1;
            delta_n = delta / n;
            delta_n2 = delta_n * delta_n;
            term1 = delta * delta_n * n1;
            m1 += delta_n;
            m4 += term1 * delta_n2 * (n * n - 3 * n + 3) + 6 * delta_n2 * m2 - 4 * delta_n * m3;
            m3 += term1 * delta_n * (n - 2) - 3 * delta_n * m2;
            m2 += term1;

            if (min == null || min.compareTo(number) > 0) {
                min = number;
            }
            if (max == null || max.compareTo(number) < 0) {
                max = number;
            }
            return new Statistic<>(n, m1, m2, m3, m4, min, max);
        });
    }
}
