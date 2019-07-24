package io.smallrye.reactive;

import io.smallrye.reactive.groups.*;
import org.reactivestreams.Publisher;

public interface Multi<T> extends Publisher<T> {

    static MultiCreate createFrom() {
        return MultiCreate.INSTANCE;
    }

    MultiSubscribe<T> subscribe();

    MultiOnResult<T> onResult();
}
