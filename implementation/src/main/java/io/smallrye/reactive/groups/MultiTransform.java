package io.smallrye.reactive.groups;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.operators.MultiTransformation;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static io.smallrye.reactive.helpers.ParameterValidation.*;

public class MultiTransform<T> {

    private final Multi<T> upstream;

    public MultiTransform(Multi<T> upstream) {
        this.upstream = upstream;
    }

    public Multi<T> bySkippingFirstItems(int number) {
        return MultiTransformation.skipFirst(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> bySkippingLastItems(int number) {
        return MultiTransformation.skipLast(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> bySkippingItemsWhile(Predicate<? super T> predicate) {
        return MultiTransformation.skipWhile(upstream, nonNull(predicate, "predicate"));
    }

    public Multi<T> bySkippingItemsFor(Duration duration) {
        return MultiTransformation.skipForDuration(upstream, validate(duration, "duration"));
    }

    public Multi<T> byTakingFirstItems(int number) {
        return MultiTransformation.takeFirst(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> byTakingLastItems(int number) {
        return MultiTransformation.takeLast(upstream, positiveOrZero(number, "number"));
    }

    public Multi<T> byTakingItemsFor(Duration duration) {
        return MultiTransformation.takeForDuration(upstream, validate(duration, "duration"));
    }

    public Multi<T> byTakingItemsWhile(Predicate<? super T> predicate) {
        return MultiTransformation.takeWhile(upstream, nonNull(predicate, "predicate"));
    }

    public Multi<T> byKeepingDistinctItems() {
        return MultiTransformation.distinct(upstream);
    }

    public Multi<T> byDroppingRepetitions() {
        return MultiTransformation.dropRepetitions(upstream);
    }

    @SafeVarargs
    public final Multi<T> byMergingWith(Publisher<T>... publishers) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        list.addAll(Arrays.asList(nonNull(publishers, "publishers")));
        return Multi.createBy().merging().streams(list);
    }

    public Multi<T> byMergingWith(Iterable<Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        nonNull(iterable, "iterable").forEach(list::add);
        return Multi.createBy().merging().streams(list);
    }

}
