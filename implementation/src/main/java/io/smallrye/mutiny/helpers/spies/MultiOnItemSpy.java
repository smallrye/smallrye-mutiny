package io.smallrye.mutiny.helpers.spies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiOnItemSpy<T> extends MultiSpyBase<T> {

    private final ArrayList<T> items = new ArrayList<>();

    public void clearItems() {
        synchronized (items) {
            items.clear();
        }
    }

    public List<T> items() {
        List<T> view;
        synchronized (items) {
            view = Collections.unmodifiableList(items);
        }
        return view;
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
}
