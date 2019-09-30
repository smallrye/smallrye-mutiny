package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.smallrye.reactive.unimulti.TimeoutException;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;
import io.smallrye.reactive.unimulti.subscription.UniSubscription;

public class UniBlockingAwait {

    private UniBlockingAwait() {
        // Avoid direct instantiation.
    }

    public static <T> T await(Uni<T> upstream, Duration duration) {
        nonNull(upstream, "upstream");
        validate(duration);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> reference = new AtomicReference<>();
        AtomicReference<Throwable> referenceToFailure = new AtomicReference<>();
        nonNull(upstream, "upstream").subscribe().withSubscriber(new UniSubscriber<T>() {
            @Override
            public void onSubscribe(UniSubscription subscription) {
                // Do nothing.
            }

            @Override
            public void onItem(T item) {
                reference.set(item);
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable failure) {
                referenceToFailure.compareAndSet(null, failure);
                latch.countDown();
            }
        });

        try {
            if (duration != null) {
                if (!latch.await(duration.toMillis(), TimeUnit.MILLISECONDS)) {
                    referenceToFailure.compareAndSet(null, new TimeoutException());
                }
            } else {
                latch.await();
            }
        } catch (InterruptedException e) {
            referenceToFailure.compareAndSet(null, e);
            Thread.currentThread().interrupt();
        }

        Throwable throwable = referenceToFailure.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new CompletionException(throwable);
        } else {
            return reference.get();
        }
    }

    private static void validate(Duration duration) {
        if (duration == null) {
            return;
        }
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("`duration` must be greater than zero`");
        }
    }
}
