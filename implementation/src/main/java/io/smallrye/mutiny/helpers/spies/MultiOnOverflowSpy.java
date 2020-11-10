package io.smallrye.mutiny.helpers.spies;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.groups.MultiOverflowStrategy;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnOverflowSpy<T> extends MultiSpyBase<T> {

    private final List<T> droppedItems;
    private final Function<MultiOverflowStrategy<? extends T>, Multi<? extends T>> strategyMapper;

    public List<T> droppedItems() {
        if (droppedItems != null) {
            List<T> view;
            view = Collections.synchronizedList(droppedItems);
            return view;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void reset() {
        super.reset();
        if (droppedItems != null) {
            synchronized (droppedItems) {
                droppedItems.clear();
            }
        }
    }

    MultiOnOverflowSpy(Multi<? extends T> upstream, boolean trackItems,
            Function<MultiOverflowStrategy<? extends T>, Multi<? extends T>> strategyMapper) {
        super(upstream);
        this.strategyMapper = strategyMapper;
        if (trackItems) {
            droppedItems = new ArrayList<>();
        } else {
            droppedItems = null;
        }
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        Multi<? extends T> wrapper = strategyMapper.apply(upstream.onOverflow().invoke(item -> {
            incrementInvocationCount();
            if (droppedItems != null) {
                synchronized (droppedItems) {
                    droppedItems.add(item);
                }
            }
        }));
        nonNull(wrapper, "wrapper").subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnItemSpy{" +
                "items=" + droppedItems +
                "} " + super.toString();
    }
}
