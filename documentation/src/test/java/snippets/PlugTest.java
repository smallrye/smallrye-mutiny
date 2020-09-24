package snippets;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

public class PlugTest {

    @Test
    @Disabled
    public void plug() {
        // tag::plug[]
        Multi.createFrom()
                .range(1, 101)
                .plug(RandomDrop::new)
                .subscribe().with(System.out::println);
        // end::plug[]
    }

    // tag::custom-operator[]
    public class RandomDrop<T> extends AbstractMultiOperator<T, T> {
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
    // end::custom-operator[]
}
