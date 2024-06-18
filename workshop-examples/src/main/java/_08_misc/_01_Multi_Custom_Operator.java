///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.6.1
package _08_misc;

import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class _01_Multi_Custom_Operator {

    public static void main(String[] args) {
        System.out.println("⚡️ Custom operator, randomly drop items");

        Multi.createFrom().range(1, 20)
                .plug(RandomDrop::new)
                .subscribe().with(System.out::println);
    }

    static class RandomDrop<T> extends AbstractMultiOperator<T, T> {
        public RandomDrop(Multi<? extends T> upstream) {
            super(upstream);
        }

        @Override
        public void subscribe(MultiSubscriber<? super T> downstream) {
            upstream.subscribe().withSubscriber(new DropProcessor(downstream));
        }

        private class DropProcessor extends MultiOperatorProcessor<T, T> {
            DropProcessor(MultiSubscriber<? super T> downstream) {
                super(downstream);
            }

            @Override
            public void onItem(T item) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    super.onItem(item);
                }
            }
        }
    }
}
