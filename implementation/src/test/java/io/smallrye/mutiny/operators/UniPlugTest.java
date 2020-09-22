package io.smallrye.mutiny.operators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

class UniPlugTest {

    @Test
    @DisplayName("Plug the custom Greeter operator and emit an item")
    void plugGreeterOnSuccess() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().item("you")
                .plug(Greeter::new)
                .onItem().transform(String::toUpperCase)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertCompletedSuccessfully().assertItem("HELLO YOU!");
    }

    @Test
    @DisplayName("Plug the custom Greeter operator and emit a failure")
    void plugGreeterOnFailure() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().failure(new RuntimeException("boom"))
                .plug(Greeter::new)
                .onItem().transform(String::toUpperCase)
                .subscribe().withSubscriber(subscriber);

        subscriber.assertFailure(RuntimeException.class, "boom");
    }

    static private class Greeter<T> extends UniOperator<T, String> {

        public Greeter(Uni<? extends T> upstream) {
            super(upstream);
        }

        @Override
        protected void subscribing(UniSerializedSubscriber<? super String> subscriber) {
            upstream().subscribe().withSubscriber(new UniDelegatingSubscriber<T, String>(subscriber) {
                @Override
                public void onItem(T item) {
                    subscriber.onItem("Hello " + item + "!");
                }
            });
        }
    }
}
