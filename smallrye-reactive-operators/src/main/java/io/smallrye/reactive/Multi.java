package io.smallrye.reactive;

import io.smallrye.reactive.groups.MultiCreate;
import io.smallrye.reactive.groups.MultiOnResult;
import io.smallrye.reactive.groups.MultiSubscribe;
import org.reactivestreams.Publisher;

public interface Multi<T> extends Publisher<T> {

    static MultiCreate createFrom() {
        return MultiCreate.INSTANCE;
    }

    MultiSubscribe<T> subscribe();

    MultiOnResult<T> onResult();

    /**
     * Creates a {@link Uni} from this {@link Multi}.
     * <p>
     * When a subscriber subscribes to the returned {@link Uni}, it subscribes to this {@link Multi} and requests one
     * result. The event emitted by this {@link Multi} are then forwarded to the {@link Uni}:
     *
     * <ul>
     * <li>on result event, the result is fired by the produced {@link Uni}</li>
     * <li>on failure event, the failure is fired by the produced {@link Uni}</li>
     * <li>on completion event, a {@code null} result is fired by the produces {@link Uni}</li>
     * <li>any result or failure events received after the first event is dropped</li>
     * </ul>
     * <p>
     * If the subscription on the produced {@link Uni} is cancelled, the subscription to the passed {@link Multi} is
     * also cancelled.
     *
     * @return the produced {@link Uni}
     */
    Uni<T> toUni();

    Multi<T> onCancellation(Runnable callback);
}
