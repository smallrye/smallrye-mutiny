package io.smallrye.reactive.operators;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.tuples.Functions;
import io.smallrye.reactive.tuples.Tuple3;
import org.reactivestreams.Publisher;

import static io.smallrye.reactive.helpers.ParameterValidation.nonNull;
import static io.smallrye.reactive.helpers.ParameterValidation.size;

public class MultiItemCombine3<T1, T2, T3> extends MultiItemCombineIterable {

    public MultiItemCombine3(Iterable<Publisher<?>> iterable) {
        super(iterable);
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fires a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombine3}
     */
    public MultiItemCombine3<T1, T2, T3> collectFailures() {
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
     * @return the current {@link MultiItemCombine3}
     */
    @Override
    public MultiItemCombine3<T1, T2, T3> latestItems() {
        super.latestItems();
        return this;
    }

    /**
     * @return the resulting {@link Multi}. The items are combined into a {@link Tuple3 Tuple3&lt;T1, T2, T3&gt;}.
     */
    public Multi<Tuple3<T1, T2, T3>> asTuple() {
        return using(Tuple3::of);
    }

    /**
     * Creates the resulting {@link Multi}. The items are combined using the given combinator function.
     *
     * @param combinator the combinator function, must not be {@code null}
     * @param <O>        the type of item
     * @return the resulting {@link Multi}.
     */
    @SuppressWarnings("unchecked")
    public <O> Multi<O> using(Functions.Function3<T1, T2, T3, O> combinator) {
        nonNull(combinator, "combinator");
        return super.combine(args -> {
            size(args, 3, "args");
            return combinator.apply((T1) args.get(0), (T2) args.get(1), (T3) args.get(2));
        });
    }
}
