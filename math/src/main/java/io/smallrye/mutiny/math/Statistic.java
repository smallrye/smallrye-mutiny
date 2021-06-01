package io.smallrye.mutiny.math;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import java.util.Objects;

/**
 * A state object for collecting statistics such as count, average, min, max, variance, standard deviation, skewness,
 * and kurtosis.
 *
 * <ul>
 * <li>Count: the number of element emitted by the upstream, 0 for empty streams.</li>
 * <li>Average: the average of the element emitted by the upstream, 0.0 for empty streams.</li>
 * <li>Min: the minimum element emitted by the upstream, {@code null} for empty streams.</li>
 * <li>Max: the maximum element emitted by the upstream, {@code null} for empty streams.</li>
 * <li>Variance: squared deviation of a items from the upstreams. It measures how far a set of items is spread out from their
 * average value.</li>
 * <li>Standard Deviation: measure of the dispersion of the set of items. A low standard deviation indicates that
 * the values tend to be close to the current average, while a high standard deviation indicates that the items are spread out
 * over a wider range.</li>
 * <li>Skewness: measure of the asymmetry of the distribution of the emitted items about its average. The skewness value can be
 * positive, zero, negative, or {@code NaN}.</li>
 * <li>Kurtosis: statistical measure that defines how heavily the tails of a item distribution differ from the tails of a normal
 * distribution.</li>
 * </ul>
 * <p>
 * Computation is based on https://www.johndcook.com/blog/skewness_kurtosis/.
 *
 * @param <T> the type of the element (Number and Comparable)
 */
public class Statistic<T> {

    final long n;
    final double m1;
    final double m2;
    final double m3;
    final double m4;
    final T min;
    final T max;

    public Statistic(long n, double m1, double m2, double m3, double m4, T min, T max) {
        this.n = n;
        this.m1 = m1;
        this.m2 = m2;
        this.m3 = m3;
        this.m4 = m4;
        this.min = min;
        this.max = max;
    }

    /**
     * @return the average (mean) of the emitted items.
     */
    public double getAverage() {
        return m1;
    }

    /**
     * @return the variance of the emitted items
     */
    public double getVariance() {
        return m2 / (n - 1.0);
    }

    /**
     * @return the standard deviation
     */
    public double getStandardDeviation() {
        return sqrt(getVariance());
    }

    /**
     * @return the skewness, can be {@code NaN}
     */
    public double getSkewness() {
        return sqrt(((double) n)) * m3 / pow(m2, 1.5);
    }

    /**
     * @return the kurtosis, can be {@code Nan}
     */
    public double getKurtosis() {
        return ((double) n) * m4 / (m2 * m2) - 3.0;
    }

    /**
     * @return the number of items
     */
    public long getCount() {
        return n;
    }

    /**
     * @return the minimum item, {@code null} if no items have been emitted
     */
    public T getMin() {
        return min;
    }

    /**
     * @return the maximum item, {@code null} if no items have been emitted
     */
    public T getMax() {
        return max;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Statistic<?> statistic = (Statistic<?>) o;
        return n == statistic.n && Double.compare(statistic.m1, m1) == 0
                && Double.compare(statistic.m2, m2) == 0 && Double.compare(statistic.m3, m3) == 0
                && Double.compare(statistic.m4, m4) == 0 && Objects.equals(min, statistic.min)
                && Objects.equals(max, statistic.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(n, m1, m2, m3, m4, min, max);
    }

    @Override
    public String toString() {
        return "Statistic{" +
                "size=" + n +
                ", min=" + min +
                ", max=" + max +
                ", average=" + getAverage() +
                ", variance=" + getVariance() +
                ", stdDeviation=" + getStandardDeviation() +
                ", skewness=" + getSkewness() +
                ", kurtosis=" + getKurtosis() +
                '}';
    }
}
