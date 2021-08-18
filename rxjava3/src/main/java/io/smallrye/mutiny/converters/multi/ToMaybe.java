package io.smallrye.mutiny.converters.multi;

import java.util.function.Function;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

public class ToMaybe<T> implements Function<Multi<T>, Maybe<T>> {
    @SuppressWarnings("rawtypes")
    public static final ToMaybe INSTANCE = new ToMaybe();

    private ToMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Maybe<T> apply(Multi<T> multi) {
        return Maybe.create(emitter -> {
            Cancellable cancellable = multi.subscribe().with(
                    item -> {
                        emitter.onSuccess(item);
                        emitter.onComplete();
                    },
                    emitter::onError,
                    emitter::onComplete);

            emitter.setCancellable(cancellable::cancel);
        });
    }
}
