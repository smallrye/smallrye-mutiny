package io.smallrye.mutiny.converters.uni;

import java.util.Optional;
import java.util.function.Function;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

@SuppressWarnings("rawtypes")
public class ToSingle<T> implements Function<Uni<T>, Single<Optional<T>>> {
    public static final ToSingle INSTANCE = new ToSingle();

    private ToSingle() {
        // Avoid direct instantiation
    }

    public static <R> ToSingleWithDefault<R> withDefault(R defaultValue) {
        return new ToSingleWithDefault<>(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <R> ToSingleFailOnNull<R> failOnNull() {
        return ToSingleFailOnNull.INSTANCE;
    }

    @Override
    public Single<Optional<T>> apply(Uni<T> uni) {
        return Single.fromPublisher(AdaptersToReactiveStreams.publisher(uni.map(Optional::ofNullable).convert().toPublisher()));
    }

}
