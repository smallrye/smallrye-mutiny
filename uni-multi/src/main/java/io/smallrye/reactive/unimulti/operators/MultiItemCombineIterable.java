package io.smallrye.reactive.unimulti.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.unimulti.Multi;
import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;

public class MultiItemCombineIterable {

    private boolean collectFailures;
    private boolean latest;

    private Iterable<? extends Publisher<?>> iterable;

    public MultiItemCombineIterable(Iterable<? extends Publisher<?>> iterable) {
        this.iterable = iterable;
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fires a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    public MultiItemCombineIterable collectFailures() {
        this.collectFailures = true;
        return this;
    }

    /**
     * By default, the combination logic is called with one item of each observed stream. It <em>waits</em> until
     * all the observed streams emit an item and call the combination logic. In other words, it associated the items
     * from different stream having the same <em>index</em>. If one of the stream completes, the produced stream also
     * completes.
     *
     * <p>
     * With this method, you can change this behavior and call the combination logic every time one of one of the observed
     * streams emit an item. It would call the combination logic with this new item and the latest items emitted by the
     * other streams. It wait until all the streams have emitted at least an item before calling the combination logic.
     * <p>
     * If one of the stream completes before having emitted a value, the produced streams also completes without emitting
     * a value.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    public MultiItemCombineIterable latestItems() {
        this.latest = true;
        return this;
    }

    /**
     * Sets the combination logic as parameter and returns a {@link Multi} associating the items from the observed
     * stream using this combinator.
     *
     * @param combinator the combination function, must not be {@code null}
     * @param <O>        the type of item produced by the returned {@link Multi} (the return type of the combinator)
     * @return the new {@link Multi}
     */
    public <O> Multi<O> using(Function<List<?>, O> combinator) {
        nonNull(combinator, "combinator");
        return combine(combinator);
    }

    <O> Multi<O> combine(Function<List<?>, O> combinator) {
        io.reactivex.functions.Function<Object[], O> fn = arr -> {
            List<?> args = Arrays.asList(arr);
            return combinator.apply(args);
        };

        if (latest) {
            if (collectFailures) {
                return new DefaultMulti<>(Flowable.combineLatestDelayError(iterable, fn));
            } else {
                return new DefaultMulti<>(Flowable.combineLatest(iterable, fn));
            }
        } else {
            if (collectFailures) {
                return new DefaultMulti<>(Flowable.zipIterable(iterable, fn, true, 128));
            } else {
                return new DefaultMulti<>(Flowable.zip(iterable, fn));
            }
        }
    }
}
