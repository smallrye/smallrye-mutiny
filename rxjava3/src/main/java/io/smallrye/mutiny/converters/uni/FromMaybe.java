package io.smallrye.mutiny.converters.uni;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.UniConverter;

public class FromMaybe<T> implements UniConverter<Maybe<T>, T> {

    @SuppressWarnings("rawtypes")
    public static final FromMaybe INSTANCE = new FromMaybe();

    private FromMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Uni<T> from(Maybe<T> instance) {
        return Uni.createFrom().emitter(sink -> {
            Disposable disposable = instance.subscribe(
                    sink::complete,
                    sink::fail,
                    () -> sink.complete(null));

            sink.onTermination(() -> {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            });
        });
    }
}
