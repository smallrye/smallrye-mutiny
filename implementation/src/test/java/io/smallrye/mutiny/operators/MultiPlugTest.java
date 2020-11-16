package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class MultiPlugTest {

    @Test
    @DisplayName("Plug the custom Greeter operator and emit items")
    void plugGreaterWithItems() {
        AssertSubscriber<String> subscriber = Multi.createFrom().items("a", "b", "c")
                .plug(Greeter::new)
                .onItem().transform(String::toUpperCase)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertCompleted().assertItems(
                "HELLO A!", "HELLO B!", "HELLO C!");
    }

    @Test
    @DisplayName("Plug the custom Greeter operator and emit a failure")
    void plugGreaterWithFailure() {
        AssertSubscriber<String> subscriber = Multi.createFrom().failure(new RuntimeException("boom"))
                .plug(Greeter::new)
                .onItem().transform(String::toUpperCase)
                .subscribe().withSubscriber(AssertSubscriber.create(10));

        subscriber.assertFailedWith(RuntimeException.class, "boom");
    }

    @Test
    @DisplayName("Reject null operator provider")
    void rejectNullProvider() {
        assertThatThrownBy(() -> Multi.createFrom().item("yo").plug(null).subscribe().asIterable())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operatorProvider");
    }

    @Test
    @DisplayName("Reject operator providing null")
    void rejectProvidedNull() {
        assertThatThrownBy(() -> Multi.createFrom().item("yo").plug(multi -> null).subscribe().asIterable())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multi");
    }

    static private class Greeter<T> extends AbstractMultiOperator<T, String> {

        public Greeter(Multi<? extends T> upstream) {
            super(upstream);
        }

        @Override
        public void subscribe(MultiSubscriber<? super String> downstream) {
            upstream.subscribe().withSubscriber(new GreeterProcessor<>(downstream));
        }
    }

    static class GreeterProcessor<T> extends MultiOperatorProcessor<T, String> {

        public GreeterProcessor(MultiSubscriber<? super String> downstream) {
            super(downstream);
        }

        @Override
        public void onItem(T item) {
            downstream.onItem("Hello " + item + "!");
        }
    }
}
