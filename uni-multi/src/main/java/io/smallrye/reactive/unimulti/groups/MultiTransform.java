package io.smallrye.reactive.unimulti.groups;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.operators.MultiTransformation;

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
