package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.multi.MultiDemandPausingOp;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.DemandPauser;

/**
 * Configures a pausable {@link Multi} stream.
 * <p>
 * This class allows configuring how a stream behaves when paused, including:
 * <ul>
 * <li>Initial pause state</li>
 * <li>Late subscription (delaying upstream subscription until resumed)</li>
 * <li>Buffer strategy (BUFFER, DROP, or IGNORE)</li>
 * <li>Buffer size limits</li>
 * </ul>
 *
 * @param <T> the type of items emitted by the stream
 */
public class MultiDemandPausing<T> {
    private final AbstractMulti<T> upstream;
    private boolean paused = false;
    private boolean lateSubscription = false;
    private int bufferSize = Infrastructure.getMultiOverflowDefaultBufferSize();
    private boolean unbounded = false;
    private BackPressureStrategy bufferStrategy = BackPressureStrategy.BUFFER;

    public MultiDemandPausing(AbstractMulti<T> upstream) {
        this.upstream = upstream;
    }

    /**
     * Sets the initial pause state of the stream.
     * <p>
     * When set to {@code true}, the stream starts paused and no items will flow until
     * {@link DemandPauser#resume()} is called.
     *
     * @param paused {@code true} to start paused, {@code false} to start flowing (default)
     * @return this configuration instance
     */
    @CheckReturnValue
    public MultiDemandPausing<T> paused(boolean paused) {
        this.paused = paused;
        return this;
    }

    /**
     * Delays the upstream subscription until the stream is resumed.
     * <p>
     * By default, the upstream subscription happens immediately even when starting paused.
     * When {@code lateSubscription} is {@code true} and the stream starts {@code paused}, the upstream
     * subscription is delayed until {@link DemandPauser#resume()} is called.
     * <p>
     * This is useful for hot sources where you want to avoid missing early items that would be
     * emitted before you're ready to process them.
     *
     * @param lateSubscription {@code true} to delay subscription until resumed, {@code false} for immediate subscription
     *        (default)
     * @return this configuration instance
     */
    @CheckReturnValue
    public MultiDemandPausing<T> lateSubscription(boolean lateSubscription) {
        this.lateSubscription = lateSubscription;
        return this;
    }

    /**
     * Sets the maximum buffer size for already-requested items when using {@link BackPressureStrategy#BUFFER}.
     * <p>
     * When the stream is paused, items that were already requested from upstream can be buffered.
     * <p>
     * Note: The buffer only holds items that were already requested from upstream before pausing.
     * When paused, no new requests are issued to upstream.
     *
     * @param bufferSize the maximum buffer size, must be positive
     * @return this configuration instance
     */
    @CheckReturnValue
    public MultiDemandPausing<T> bufferSize(int bufferSize) {
        this.bufferSize = positive(bufferSize, "bufferSize");
        this.unbounded = false;
        return this;
    }

    /**
     * Sets the buffer size for already-requested items to unbounded when using {@link BackPressureStrategy#BUFFER}.
     * <p>
     * When the stream is paused, items that were already requested from upstream can be buffered.
     *
     * @return this configuration instance
     */
    @CheckReturnValue
    public MultiDemandPausing<T> bufferUnconditionally() {
        this.bufferSize = Infrastructure.getMultiOverflowDefaultBufferSize();
        this.unbounded = true;
        return this;
    }

    /**
     * Sets the strategy for handling already-requested items while paused.
     * <p>
     * Available strategies:
     * <ul>
     * <li>{@link BackPressureStrategy#BUFFER}: Buffer items while paused, deliver when resumed (default)</li>
     * <li>{@link BackPressureStrategy#DROP}: Drop items while paused, continue with fresh items when resumed</li>
     * <li>{@link BackPressureStrategy#IGNORE}: Continue delivering already-requested items even while paused</li>
     * </ul>
     *
     * @param bufferStrategy the buffer strategy, must not be {@code null}
     * @return this configuration instance
     */
    @CheckReturnValue
    public MultiDemandPausing<T> bufferStrategy(BackPressureStrategy bufferStrategy) {
        this.bufferStrategy = nonNull(bufferStrategy, "bufferStrategy");
        if (bufferStrategy != BackPressureStrategy.BUFFER
                && bufferStrategy != BackPressureStrategy.DROP
                && bufferStrategy != BackPressureStrategy.IGNORE) {
            throw new IllegalArgumentException("Demand pauser only supports BUFFER, DROP or IGNORE strategy");
        }
        return this;
    }

    /**
     * Sets the demand pauser and return the new {@link Multi}.
     *
     * @param pauser the pauser handle, must not be {@code null}
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public Multi<T> using(DemandPauser pauser) {
        DemandPauser p = nonNull(pauser, "pauser");
        MultiDemandPausingOp<T> pausingMulti = new MultiDemandPausingOp<>(upstream,
                paused, lateSubscription, bufferSize, unbounded, bufferStrategy);
        p.bind(pausingMulti);
        return Infrastructure.onMultiCreation(pausingMulti);
    }

}
