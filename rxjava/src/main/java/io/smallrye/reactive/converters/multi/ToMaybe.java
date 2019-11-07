package io.smallrye.reactive.converters.multi;

import java.util.function.Function;

import io.reactivex.Maybe;
import io.smallrye.reactive.Multi;

public class ToMaybe<T> implements Function<Multi<T>, Maybe<T>> {
    public static final ToMaybe INSTANCE = new ToMaybe();

    private ToMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Maybe<T> apply(Multi<T> multi) {
        return Maybe.create(emitter -> {
            multi.subscribe().with(
                    item -> {
                        emitter.onSuccess(item);
                        emitter.onComplete();
                    },
                    emitter::onError,
                    emitter::onComplete);
        });
    }
}
