package io.smallrye.mutiny.math;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import io.smallrye.mutiny.Multi;

public class TopOperator<T extends Comparable<T>> implements Function<Multi<T>, Multi<List<T>>> {

    private final int count;
    private final SortedSet<T> list;

    public TopOperator(int count) {
        this.count = count;
        this.list = new ConcurrentSkipListSet<>(Comparator.reverseOrder());
    }

    @Override
    public Multi<List<T>> apply(Multi<T> multi) {
        return multi
                .onTermination().invoke(list::clear)
                .onItem().transformToMultiAndConcatenate(item -> {
                    // Add the item
                    if (!this.list.add(item)) {
                        // not inserted
                        return Multi.createFrom().empty();
                    }

                    // Overflow case
                    if (this.list.size() > count) {
                        // Get last and remove it
                        T last = this.list.last();
                        this.list.remove(last);
                        // It the given item was the last one, nothing to do, otherwise emit
                        if (last != item) {
                            return Multi.createFrom().item(new ArrayList<>(this.list));
                        }
                        return Multi.createFrom().empty();
                    } else {
                        return Multi.createFrom().item(new ArrayList<>(this.list));
                    }
                });
    }
}
