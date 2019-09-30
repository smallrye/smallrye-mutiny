package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.size;

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.tuples.Tuple2;

public class MultiItemCombine2<T1, T2> extends MultiItemCombineIterable {

    public MultiItemCombine2(Iterable<Publisher<?>> iterable) {
        super(iterable);
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fires a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombine2}
     */
    @Override
    public MultiItemCombine2<T1, T2> collectFailures() {
        super.collectFailures();
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
     * @return the current {@link MultiItemCombine2}
     */
    @Override
    public MultiItemCombine2<T1, T2> latestItems() {
        super.latestItems();
        return this;
    }

    /**
     * @return the resulting {@link Multi}. The items are combined into a {@link Tuple2 Tuple2&lt;T1, T2&gt;}.
     */
    public Multi<Tuple2<T1, T2>> asTuple() {
        return using(Tuple2::of);
    }

    /**
     * Creates the resulting {@link Multi}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Multi}.
     */
    @SuppressWarnings("unchecked")
    public <O> Multi<O> using(BiFunction<T1, T2, O> combinator) {
        nonNull(combinator, "combinator");
        return super.combine(args -> {
            size(args, 2, "args");
            return combinator.apply((T1) args.get(0), (T2) args.get(1));
        });
    }
}
