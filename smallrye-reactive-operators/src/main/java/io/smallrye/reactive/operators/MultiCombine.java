package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.Multi;
import io.smallrye.reactive.helpers.ParameterValidation;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.stream.Collectors;

public class MultiCombine {

    private MultiCombine() {
        // avoid direct instantiation.
    }


    @SuppressWarnings("unchecked")
    private static <T> Flowable<T> getFlowable(Publisher<T> publisher) {
        if (publisher instanceof Flowable) {
            return (Flowable) publisher;
        }
        if (publisher instanceof Multi) {
            return MultiCollector.getFlowable((Multi) publisher);
        }
        return Flowable.fromPublisher(publisher);
    }

    public static <T> Multi<T> merge(List<Publisher<T>> participants, boolean collectFailures, int requests, int concurrency) {
        List<Flowable<T>> flowables = ParameterValidation.doesNotContainNull(participants, "participants")
                .stream().map(MultiCombine::getFlowable).collect(Collectors.toList());
        Flowable<T> merged;
        if (collectFailures) {
            merged = Flowable.mergeDelayError(flowables, concurrency, requests);
        } else {
            merged = Flowable.merge(flowables, concurrency, requests);
        }
        return new DefaultMulti<>(merged);
    }

    public static <T> Multi<T> concatenate(List<Publisher<T>> participants, boolean collectFailures, int requests) {
        List<Flowable<T>> flowables = ParameterValidation.doesNotContainNull(participants, "participants")
                .stream().map(MultiCombine::getFlowable).collect(Collectors.toList());
        Flowable<T> concatenated;
        if (collectFailures) {
            concatenated = Flowable.concatDelayError(Flowable.fromIterable(flowables), requests, true);
        } else {
            concatenated = Flowable.concat(Flowable.fromIterable(flowables), requests);
        }
        return new DefaultMulti<>(concatenated);
    }

}
