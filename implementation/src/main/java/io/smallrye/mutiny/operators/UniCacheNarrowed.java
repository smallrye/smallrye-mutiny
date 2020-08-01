package io.smallrye.mutiny.operators;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiPredicate;

import io.smallrye.mutiny.Uni;

/**
 * A {@link UniCache} implementation that keeps items at most a given duration
 * and resubscribes to the upstream after the duration has exceeded.
 */
public class UniCacheNarrowed<I> extends UniCache<I> {

    private final UniCacheNarrowedValidator<I> validator;

    public UniCacheNarrowed(Uni<? extends I> upstream, Duration validity) {
        this(upstream, new UniCacheNarrowedValidator<>(validity));
    }

    private UniCacheNarrowed(Uni<? extends I> upstream, UniCacheNarrowedValidator<I> validator) {
        super(upstream, validator);
        this.validator = validator;
    }

    @Override
    public void onItem(I item) {
        super.onItem(item);
        validator.refreshEventTime();
    }
}

/**
 * {@link UniCache} validator implementation for event age related validation.
 */
class UniCacheNarrowedValidator<I> implements BiPredicate<I, Throwable> {

    private final Duration validity;
    private Instant cacheExpiration = Instant.MIN;

    UniCacheNarrowedValidator(Duration validity) {
        this.validity = validity;
    }

    synchronized void refreshEventTime() {
        this.cacheExpiration = Instant.now().plus(validity);
    }

    @Override
    public synchronized boolean test(I i, Throwable throwable) {
        return Instant.now().isBefore(cacheExpiration);
    }
}
