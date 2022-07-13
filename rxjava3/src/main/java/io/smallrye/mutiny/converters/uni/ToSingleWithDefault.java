package io.smallrye.mutiny.converters.uni;

import java.util.function.Function;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

public class ToSingleWithDefault<T> implements Function<Uni<T>, Single<T>> {

    private final T defaultValue;

    public ToSingleWithDefault(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public Single<T> apply(Uni<T> uni) {
        return Single.fromPublisher(
                AdaptersToReactiveStreams.publisher(uni.onItem().ifNull().continueWith(defaultValue).convert().toPublisher()));
    }
}
