package io.smallrye.mutiny.helpers;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ExponentialBackoff {

    public static final Duration MAX_BACKOFF = Duration.ofMillis(Long.MAX_VALUE);
    public static final double DEFAULT_JITTER = 0.5;

    private ExponentialBackoff() {
        // avoid direct instantiation
    }

    /**
     * Computes a method that would delay <em>ticks</em> using an exponential backoff.
     *
     * @param numRetries the max number of retries
     * @param firstBackoff the delay of the first backoff
     * @param maxBackoff the max backoff
     * @param jitterFactor the jitter factor in [0, 1]
     * @param executor the executor used for the delay
     * @return the function
     */
    public static Function<Multi<Throwable>, Publisher<Long>> randomExponentialBackoffFunction(
            long numRetries, Duration firstBackoff, Duration maxBackoff,
            double jitterFactor, ScheduledExecutorService executor) {

        validate(firstBackoff, maxBackoff, jitterFactor, executor);

        AtomicInteger index = new AtomicInteger();
        return t -> t
                .onItem().transformToUni(failure -> {
                    int iteration = index.incrementAndGet();
                    if (iteration >= numRetries) {
                        return Uni.createFrom().<Long> failure(
                                new IllegalStateException("Retries exhausted: " + iteration + "/" + numRetries,
                                        failure));
                    }

                    Duration delay = getNextDelay(firstBackoff, maxBackoff, jitterFactor, iteration);
                    return Uni.createFrom().item((long) iteration).onItem().delayIt()
                            .onExecutor(executor).by(delay);
                }).concatenate();
    }

    private static Duration getNextDelay(Duration firstBackoff, Duration maxBackoff, double jitterFactor, int iteration) {
        Duration nextBackoff = getNextAttemptDelay(firstBackoff, maxBackoff, iteration);

        // Compute the jitter
        long jitterOffset = getJitter(jitterFactor, nextBackoff);
        long lowBound = Math.max(firstBackoff.minus(nextBackoff).toMillis(), -jitterOffset);
        long highBound = Math.min(maxBackoff.minus(nextBackoff).toMillis(), jitterOffset);
        long jitter;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (highBound == lowBound) {
            if (highBound == 0) {
                jitter = 0;
            } else {
                jitter = random.nextLong(highBound);
            }
        } else {
            jitter = random.nextLong(lowBound, highBound);
        }

        return nextBackoff.plusMillis(jitter);
    }

    private static void validate(Duration firstBackoff, Duration maxBackoff, double jitterFactor,
            ScheduledExecutorService executor) {
        if (jitterFactor < 0 || jitterFactor > 1) {
            throw new IllegalArgumentException("jitterFactor must be between 0 and 1 (default 0.5)");
        }
        ParameterValidation.nonNull(firstBackoff, "firstBackoff");
        ParameterValidation.nonNull(maxBackoff, "maxBackoff");
        ParameterValidation.nonNull(executor, "executor");
    }

    /**
     * Computes a method that would delay <em>ticks</em> using an exponential backoff.
     * Will keep retrying until an expiration time.
     * The last attempt will start before the expiration time.
     *
     * @param expireAt absolute time in millis that specifies when to give up.
     * @param firstBackoff the delay of the first backoff
     * @param maxBackoff the max backoff
     * @param jitterFactor the jitter factor in [0, 1]
     * @param executor the executor used for the delay
     * @return the function
     */
    public static Function<Multi<Throwable>, Publisher<Long>> randomExponentialBackoffFunctionExpireAt(
            long expireAt, Duration firstBackoff, Duration maxBackoff,
            double jitterFactor, ScheduledExecutorService executor) {

        validate(firstBackoff, maxBackoff, jitterFactor, executor);

        AtomicInteger index = new AtomicInteger();
        return t -> t
                .onItem().transformToUni(failure -> {
                    int iteration = index.incrementAndGet();
                    Duration delay = getNextDelay(firstBackoff, maxBackoff, jitterFactor, iteration);

                    long checkTime = System.currentTimeMillis() + delay.toMillis();
                    if (checkTime > expireAt) {
                        return Uni.createFrom().<Long> failure(
                                new IllegalStateException(
                                        "Retries exhausted : " + iteration + " attempts against " + checkTime + "/" + expireAt
                                                + " expiration",
                                        failure));
                    }
                    return Uni.createFrom().item((long) iteration).onItem().delayIt()
                            .onExecutor(executor).by(delay);
                }).concatenate();
    }

    private static long getJitter(double jitterFactor, Duration nextBackoff) {
        long jitterOffset;
        try {
            jitterOffset = nextBackoff.multipliedBy((long) (100 * jitterFactor))
                    .dividedBy(100)
                    .toMillis();
        } catch (ArithmeticException ae) {
            jitterOffset = Math.round(Long.MAX_VALUE * jitterFactor);
        }
        return jitterOffset;
    }

    private static Duration getNextAttemptDelay(Duration firstBackoff, Duration maxBackoff, int iteration) {
        Duration nextBackoff;
        try {
            nextBackoff = firstBackoff.multipliedBy((long) Math.pow(2, iteration));
            if (nextBackoff.compareTo(maxBackoff) > 0) {
                nextBackoff = maxBackoff;
            }
        } catch (ArithmeticException overflow) {
            nextBackoff = maxBackoff;
        }
        return nextBackoff;
    }
}
