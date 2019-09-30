package io.smallrye.reactive.unimulti.operators;

import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.unimulti.helpers.ParameterValidation.size;

import org.reactivestreams.Publisher;

import io.smallrye.reactive.unimulti.Multi;
import io.smallrye.reactive.unimulti.tuples.Functions;
import io.smallrye.reactive.unimulti.tuples.Tuple5;

public class MultiItemCombine5<T1, T2, T3, T4, T5> extends MultiItemCombineIterable {

    public MultiItemCombine5(Iterable<Publisher<?>> iterable) {
        super(iterable);
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fires a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombine5}
     */
    public MultiItemCombine5<T1, T2, T3, T4, T5> collectFailures() {
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
     * @return the current {@link MultiItemCombine5}
     */
    @Override
    public MultiItemCombine5<T1, T2, T3, T4, T5> latestItems() {
        super.latestItems();
        return this;
    }

    /**
     * @return the resulting {@link Multi}. The items are combined into a {@link Tuple5 Tuple5&lt;T1, T2, T3, T4, T5&gt;}.
     */
    public Multi<Tuple5<T1, T2, T3, T4, T5>> asTuple() {
        return using(Tuple5::of);
    }

    /**
     * Creates the resulting {@link Multi}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O> the type of item
     * @return the resulting {@link Multi}.
     */
    @SuppressWarnings("unchecked")
    public <O> Multi<O> using(Functions.Function5<T1, T2, T3, T4, T5, O> combinator) {
        nonNull(combinator, "combinator");
        return super.combine(args -> {
            size(args, 5, "args");
            return combinator
                    .apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2), (T4) args.get(3), (T5) args.get(4));
        });
    }
}
