package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.Multi;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

@SuppressWarnings("rawtypes")
public class ToFlowable<T> implements Function<Multi<T>, Flowable<T>> {
    public static final ToFlowable INSTANCE = new ToFlowable();

    private ToFlowable() {
        // Avoid direct instantiation
    }

    @Override
    public Flowable<T> apply(Multi<T> multi) {
        return Flowable.fromPublisher(AdaptersToReactiveStreams.publisher(multi));
    }
}
