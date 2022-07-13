package io.smallrye.mutiny.infrastructure;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;

import io.smallrye.mutiny.Multi;

/**
 * Allow being notified when a new {@link Multi} instance is created and when this {@link Multi} receives events.
 * <p>
 * Implementations are expected to be exposed as SPI, and so the implementation class must be declared in the
 * {@code META-INF/services/io.smallrye.mutiny.infrastructure.MultiInterceptor} file.
 */
public interface MultiInterceptor extends MutinyInterceptor {

    /**
     * Method called when a new instance of {@link Multi} is created. If can return a new {@code Multi},
     * or the passed {@code Multi} (default behavior) if the interceptor is not interested by this {@code Multi}.
     * <p>
     * One use case for this method is the capture of a context at creation time (when the method is called) and
     * restored when a subscriber subscribed to the produced {@code multi}. It is recommended to extend
     * {@link io.smallrye.mutiny.operators.AbstractMulti} to produce a new {@link Multi} instance.
     *
     * @param multi the created multi
     * @param <T> the type of item produced by the multi
     * @return the passed multi or a new instance, must not be {@code null}
     */
    default <T> Multi<T> onMultiCreation(Multi<T> multi) {
        return multi;
    }

    /**
     * Method called when a subscriber subscribes to a {@link Multi}.
     * This method lets you substitute the subscriber.
     *
     * @param instance the instance of publisher
     * @param subscriber the subscriber
     * @param <T> the type of item
     * @return the subscriber to use instead of the passed one. By default, it returns the given subscriber.
     */
    default <T> Subscriber<? super T> onSubscription(Flow.Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        return subscriber;
    }

}
