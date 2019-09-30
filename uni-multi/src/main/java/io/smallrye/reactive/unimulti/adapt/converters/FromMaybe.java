package io.smallrye.reactive.unimulti.adapt.converters;

import io.reactivex.Maybe;
import io.reactivex.disposables.Disposable;
import io.smallrye.reactive.unimulti.Uni;
import io.smallrye.reactive.unimulti.adapt.UniConverter;

public class FromMaybe<T> implements UniConverter<Maybe<T>, T> {
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
