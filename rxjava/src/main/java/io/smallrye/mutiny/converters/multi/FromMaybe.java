package io.smallrye.mutiny.converters.multi;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.MultiConverter;

public class FromMaybe<T> implements MultiConverter<Maybe<T>, T> {

    public static final FromMaybe INSTANCE = new FromMaybe();

    private FromMaybe() {
        // Avoid direct instantiation
    }

    @Override
    public Multi<T> from(Maybe<T> instance) {
        return Multi.createFrom().emitter(sink -> {
            Disposable disposable = instance.subscribe(
                    item -> {
                        sink.emit(item);
                        sink.complete();
                    },
                    sink::fail,
                    sink::complete);

            sink.onTermination(() -> {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            });
        });
    }
}
