package io.smallrye.mutiny.groups;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.MultiCombineLatestOp;
import io.smallrye.mutiny.operators.multi.MultiZipOp;

public class MultiItemCombineIterable {

    private boolean collectFailures;
    private boolean latest;

    private final Iterable<? extends Publisher<?>> iterable;

    public MultiItemCombineIterable(Iterable<? extends Publisher<?>> iterable) {
        this.iterable = iterable;
    }

    /**
     * Configures the combination to wait until all the {@link Publisher streams} to fire a completion or failure event
     * before propagating a failure downstream.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    @CheckReturnValue
    public MultiItemCombineIterable collectFailures() {
        this.collectFailures = true;
        return this;
    }

    /**
     * By default, the combination logic is called with one item of each observed stream. It <em>waits</em> until all
     * the upstreams emit an item, and it invokes the combination logic. In other words, it associated the items from
     * different streams having the same <em>index</em>. If one of the streams completes, the produced stream also
     * completes.
     * <p>
     * With this method, you can change this behavior and call the combination logic when one of the observed streams
     * emits an item. It calls the combination logic with this new item and the latest items emitted by the other
     * streams.
     * <p>
     * It waits until all the streams have emitted at least one item before calling the combination logic.
     * If one of the streams completes before emitting a value, the produced stream also completes without emitting an
     * item.
     *
     * @return the current {@link MultiItemCombineIterable}
     */
    @CheckReturnValue
    public MultiItemCombineIterable latestItems() {
        this.latest = true;
        return this;
    }

    /**
     * Sets the combination logic as parameter and returns a {@link Multi} associating the items from the observed
     * stream using this combinator.
     *
     * @param combinator the combination function, must not be {@code null}
     * @param <O> the type of item produced by the returned {@link Multi} (the return type of the combinator)
     * @return the new {@link Multi}
     */
    @CheckReturnValue
    public <O> Multi<O> using(Function<List<?>, O> combinator) {
        nonNull(combinator, "combinator");
        return combine(combinator);
    }

    @CheckReturnValue
    <O> Multi<O> combine(Function<List<?>, ? extends O> combinator) {
        if (latest) {
            if (collectFailures) {
                return Infrastructure.onMultiCreation(new MultiCombineLatestOp<>(iterable, combinator, 128, true));
            } else {
                return Infrastructure.onMultiCreation(new MultiCombineLatestOp<>(iterable, combinator, 128, false));
            }
        } else {
            if (collectFailures) {
                return Infrastructure.onMultiCreation(new MultiZipOp<>(iterable, combinator, 128, true));
            } else {
                return Infrastructure.onMultiCreation(new MultiZipOp<>(iterable, combinator, 128, false));
            }
        }
    }
}
