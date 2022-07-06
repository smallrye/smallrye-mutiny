package guides;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("InnerClassMayBeStatic")
@ExtendWith(SystemOutCaptureExtension.class)
public class PlugTest {

    @Test
    public void plug(SystemOut out) {
        // <plug>
        Multi.createFrom()
                .range(1, 101)
                .plug(RandomDrop::new)
                .subscribe().with(System.out::println);
        // </plug>

        assertThat(out.get()).isNotBlank();
    }

    // <custom-operator>
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
    // </custom-operator>
}
