package io.smallrye.reactive.unimulti.infrastructure;

import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.operators.AbstractUni;
import io.smallrye.reactive.unimulti.subscription.UniSubscriber;

/**
 * Allow being notified when a new {@link Uni} instance is created and when this {@link Uni} receives events.
 * <p>
 * Implementations are expected to be exposed as SPI, and so the implementation class must be declared in the
 * {@code META-INF/services/io.smallrye.reactive.infrastructure.UniInterceptor} file.
 */
public interface UniInterceptor {

    /**
     * @return the interceptor ordinal. The ordinal is used to sort the interceptor. Lower value are executed first.
     * Default is 100.
     */
    default int ordinal() {
        return 100;
    }

    /**
     * Method called when a new instance of {@link Uni} is created. If can return a new {@code Uni},
     * or the passed {@code Uni} (default behavior) if the interceptor is not interested by this {@code uni}.
     * <p>
     * One use case for this method is the capture of a context at creation time (when the method is called) and
     * restored when a subscriber subscribed to the produced {@code uni}. It is recommended to extend
     * {@link AbstractUni} to produce a new {@link Uni} instance.
     *
     * @param uni the created uni
     * @param <T> the type of item produced by the uni
     * @return the passed uni or a new instance, must not be {@code null}
     */
    default <T> Uni<T> onUniCreation(Uni<T> uni) {
        return uni;
    }

    /**
     * Method called when a subscriber subscribes to a {@link Uni}.
     * This method lets you substitute the subscriber.
     *
     * @param instance   the instance of uni
     * @param subscriber the subscriber
     * @param <T>        the type of item
     * @return the subscriber to use instead of the passed one. By default, it returns the pass subscriber.
     */
    default <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        return subscriber;
    }

}
