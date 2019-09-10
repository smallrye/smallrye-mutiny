package io.smallrye.reactive.adapt;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.smallrye.reactive.Uni;

import java.util.concurrent.CompletableFuture;

/**
 * {@link UniAdapter} implementation for the {@link Maybe} type.
 */
public class MaybeUniAdapter implements UniAdapter<Maybe<?>> {
    @Override
    public boolean accept(Class<Maybe<?>> clazz) {
        return Maybe.class.isAssignableFrom(clazz);
    }

    @Override
    public Uni<?> adaptFrom(Maybe<?> instance) {
        return Uni.createFrom().emitter(sink -> {
            Disposable disposable = instance.subscribe(
                    sink::complete,
                    sink::fail,
                    () -> sink.complete(null)
            );

            sink.onTermination(() -> {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            });
        });
    }
}
