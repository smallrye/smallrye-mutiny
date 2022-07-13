package io.smallrye.mutiny.converters.uni;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.concurrent.Flow;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

// Several important points to note here
// 1. The subscription on this Uni must be done when we receive a request, not on the subscription
// 2. The request parameter must be checked to be compliant with Reactive Streams
// 3. Cancellation can happen 1) before the request (and so the uni subscription); 2) after the request but
// before the emission; 3) after the emission. In (1) the uni subscription must not happen. In (2), the emission
// must not happen. In (3), the emission could happen.
// 4. If the uni item is `null` the stream is completed. If the uni item is not `null`, the stream contains
// the item and the end of stream signal. In the case of error, the stream propagates the error.

public class ToPublisher<T> implements Function<Uni<T>, Flow.Publisher<T>> {

    public static final ToPublisher INSTANCE = new ToPublisher();

    private ToPublisher() {
        // Avoid direct instantiation
    }

    @Override
    public Flow.Publisher<T> apply(Uni<T> uni) {
        //return new UniPublisher<>(nonNull(uni, "uni"));
        return new UniToMultiPublisher<>(nonNull(uni, "uni"));
    }

}
