package io.smallrye.mutiny.groups;

import java.util.function.Consumer;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.overflow.MultiOnOverflowDropItemsOp;

public class MultiOverflowStrategy<T> {

    private final Multi<T> upstream;
    private final Consumer<T> dropConsumer;
    private final Function<T, Uni<?>> dropUniMapper;

    public MultiOverflowStrategy(Multi<T> upstream, Consumer<T> dropConsumer, Function<T, Uni<?>> dropUniMapper) {
        // No need for null checks since MultiOverflow must have performed them
        this.upstream = upstream;
        this.dropConsumer = dropConsumer;
        this.dropUniMapper = dropUniMapper;
    }

    /**
     * When the downstream cannot keep up with the upstream emissions, instruct to drop the item.
     *
     * @return the new multi
     */
    public Multi<T> drop() {
        if (dropConsumer != null) {
            return Infrastructure.onMultiCreation(new MultiOnOverflowDropItemsOp<>(upstream, dropConsumer));
        } else {
            return Infrastructure.onMultiCreation(new MultiOnOverflowDropItemsOp<>(upstream, dropUniMapper));
        }
    }
}
