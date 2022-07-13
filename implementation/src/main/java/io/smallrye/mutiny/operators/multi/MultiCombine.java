package io.smallrye.mutiny.operators.multi;

import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.ParameterValidation;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.builders.CollectionBasedMulti;

public class MultiCombine {

    private MultiCombine() {
        // avoid direct instantiation.
    }

    public static <T> Multi<T> merge(List<Publisher<T>> participants, boolean collectFailures, int requests,
            int concurrency) {
        List<Publisher<T>> candidates = ParameterValidation.doesNotContainNull(participants, "participants");

        if (participants.isEmpty()) {
            return Multi.createFrom().empty();
        }
        if (participants.size() == 1) {
            return Multi.createFrom().publisher(participants.get(0));
        }
        if (collectFailures) {
            return Infrastructure.onMultiCreation(new CollectionBasedMulti<>(candidates)
                    .onItem().transformToMulti(Function.identity()).collectFailures().withRequests(requests)
                    .merge(concurrency));
        } else {
            return Infrastructure.onMultiCreation(new CollectionBasedMulti<>(candidates)
                    .onItem().transformToMulti(Function.identity()).withRequests(requests).merge(concurrency));
        }
    }
}
