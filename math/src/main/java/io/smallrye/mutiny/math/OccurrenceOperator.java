package io.smallrye.mutiny.math;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

public class OccurrenceOperator<T> implements Function<Multi<T>, Multi<Map<T, Long>>> {

    private final ConcurrentHashMap<T, Long> occurrences = new ConcurrentHashMap<>();

    @Override
    public Multi<Map<T, Long>> apply(Multi<T> multi) {
        return multi
                .onTermination().invoke(occurrences::clear)
                .onItem().transform(item -> {
                    occurrences.compute(item, (it, c) -> c == null ? 1 : c + 1);
                    return (Map<T, Long>) new HashMap<>(occurrences);
                })
                .onCompletion().ifEmpty().continueWith(Collections.emptyMap());
    }
}
