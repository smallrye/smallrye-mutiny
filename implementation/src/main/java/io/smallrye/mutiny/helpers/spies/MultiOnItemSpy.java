package io.smallrye.mutiny.helpers.spies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnItemSpy<T> extends MultiSpyBase<T> {

    private final ArrayList<T> items = new ArrayList<>();

    public List<T> items() {
        List<T> view;
        view = Collections.synchronizedList(items);
        return view;
    }

    @Override
    public void reset() {
        super.reset();
        synchronized (items) {
            items.clear();
        }
    }

    MultiOnItemSpy(Multi<? extends T> upstream) {
        super(upstream);
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> downstream) {
        upstream.onItem().invoke(item -> {
            incrementInvocationCount();
            synchronized (items) {
                items.add(item);
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
