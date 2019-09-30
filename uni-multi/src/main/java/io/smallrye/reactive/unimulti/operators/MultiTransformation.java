package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;
import org.reactivestreams.Publisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.smallrye.reactive.unimulti.operators.MultiCollector.getFlowable;

public class MultiTransformation {

    private MultiTransformation() {
        // avoid direct instantiation
    }

    public static <T> Multi<T> skipFirst(Multi<T> upstream, int number) {
        return new DefaultMulti<>(getFlowable(upstream).skip(number));
    }

    public static <T> Multi<T> skipLast(Multi<T> upstream, int number) {
        return new DefaultMulti<>(getFlowable(upstream).skipLast(number));
    }

    public static <T> Multi<T> skipForDuration(Multi<T> upstream, Duration duration) {
        return new DefaultMulti<>(getFlowable(upstream).skip(duration.toMillis(), TimeUnit.MILLISECONDS));
    }

    public static <T> Multi<T> skipWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return new DefaultMulti<>(getFlowable(upstream).skipWhile(predicate::test));
    }

    public static <T> Multi<T> takeFirst(Multi<T> upstream, int number) {
        return new DefaultMulti<>(getFlowable(upstream).take(number));
    }

    public static <T> Multi<T> takeLast(Multi<T> upstream, int number) {
        return new DefaultMulti<>(getFlowable(upstream).takeLast(number));
    }

    public static <T> Multi<T> takeForDuration(Multi<T> upstream, Duration duration) {
        return new DefaultMulti<>(getFlowable(upstream).take(duration.toMillis(), TimeUnit.MILLISECONDS));
    }

    public static <T> Multi<T> takeWhile(Multi<T> upstream, Predicate<? super T> predicate) {
        return new DefaultMulti<>(getFlowable(upstream).takeWhile(predicate::test));
    }

    public static <T> Multi<T> distinct(Multi<T> upstream) {
        return new DefaultMulti<>(getFlowable(upstream).distinct());
    }

    public static <T> Multi<T> dropRepetitions(Multi<T> upstream) {
        return new DefaultMulti<>(getFlowable(upstream).distinctUntilChanged());
    }

    public static <T> Multi<T> merge(Multi<T> upstream, Publisher<T>... publishers) {
       return merge(upstream, Arrays.asList(publishers));
    }

    public static <T> Multi<T> merge(Multi<T> upstream, Iterable<Publisher<T>> iterable) {
        List<Publisher<T>> list = new ArrayList<>();
        list.add(upstream);
        iterable.forEach(list::add);
        Flowable<T> merged = Flowable.merge(list);
        return new DefaultMulti<>(merged);
    }


}
