package io.smallrye.mutiny.helpers.spies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnItemSpy<T> extends MultiSpyBase<T> {

    private final List<T> items;

    public List<T> items() {
        if (items != null) {
            List<T> view;
            view = Collections.synchronizedList(items);
            return view;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void reset() {
        super.reset();
        if (items != null) {
            synchronized (items) {
                items.clear();
            }
        }
    }

    MultiOnItemSpy(Multi<? extends T> upstream, boolean trackItems) {
        super(upstream);
        if (trackItems) {
            items = new ArrayList<>();
        } else {
            items = null;
        }
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onItem().invoke(item -> {
            incrementInvocationCount();
            if (items != null) {
                synchronized (items) {
                    items.add(item);
                }
            }
        }).subscribe().withSubscriber(downstream);
    }

    @Override
    public String toString() {
        return "MultiOnItemSpy{" +
                "items=" + items +
                "} " + super.toString();
    }
}
