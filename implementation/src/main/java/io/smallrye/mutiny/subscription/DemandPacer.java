package io.smallrye.mutiny.subscription;

import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.smallrye.mutiny.Multi;

/**
 * Interface for {@link Multi} demand pacers and the {@link Multi#paceDemand()} operator.
 * <p>
 * A demand-pacer allows controlling upstream demand using a request and a delay.
 * Each time the delay expires the pacer can evaluate a new demand based on the previous request and the number of emitted items
 * that have been observed since the previous request.
 * <p>
 * The {@link FixedDemandPacer} offers a fixed delay / fixed demand implementation, but custom / adaptive strategies can be
 * provided through this {@link DemandPacer} interface.
 */
public interface DemandPacer {

    /**
     * A demand request.
     */
    class Request {

        private final long demand;
        private final Duration delay;

        /**
         * Constructs a new request.
         *
         * @param demand the number of items, must be strictly positive
         * @param delay the delay, must not be {@code null}, must be strictly positive
         */
        public Request(long demand, Duration delay) {
            this.demand = positive(demand, "demand");
            this.delay = validate(delay, "delay");
            if (delay == ChronoUnit.FOREVER.getDuration()) {
                throw new IllegalArgumentException("ChronoUnit.FOREVER is not a correct delay value");
            }
        }

        /**
         * Get the demand.
         *
         * @return the demand
         */
        public long demand() {
            return demand;
        }

        /**
         * Get the delay
         *
         * @return the delay
         */
        public Duration delay() {
            return delay;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "demand=" + demand +
                    ", delay=" + delay +
                    '}';
        }
    }

    /**
     * Get the initial request.
     * <p>
     * This will be called at the {@link Multi#paceDemand()} operator subscription time.
     *
     * @return the request, must not be {@code null}
     */
    Request initial();

    /**
     * Evaluate the next request after the previous request delay has expired.
     *
     * @param previousRequest the previous request
     * @param observedItemsCount the number of emitted items that have been observed since the last request
     * @return the request, must not be {@code null}
     */
    Request apply(Request previousRequest, long observedItemsCount);
}
