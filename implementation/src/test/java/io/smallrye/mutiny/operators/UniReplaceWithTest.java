package io.smallrye.mutiny.operators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

class UniReplaceWithTest {

    @Test
    @DisplayName("Replace with a value")
    void replaceWithValue() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .item(69)
                .replaceWith(63)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(63);
    }

    @Test
    @DisplayName("Replace with a supplier")
    void replaceWithSupplier() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .item(69)
                .replaceWith(() -> 63)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(63);
    }

    @Test
    @DisplayName("Replace with a Uni")
    void replaceWithUni() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .item(69)
                .replaceWith(Uni.createFrom().item(63))
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(63);
    }

    @Test
    @DisplayName("Replace with null")
    void replaceWithNull() {
        UniAssertSubscriber<Integer> subscriber = Uni.createFrom()
                .item(69)
                .replaceWithNull()
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(null);
    }
}
