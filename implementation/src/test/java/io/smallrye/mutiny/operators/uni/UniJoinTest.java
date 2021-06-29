package io.smallrye.mutiny.operators.uni;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.spies.UniOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.UniOnItemSpy;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

class UniJoinTest {

    @Nested
    class JoinAll {

        @Test
        void joinItems() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c);

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2, 3));
        }

        @Test
        void joinNumericTypesItems() {
            Uni<Number> a = Uni.createFrom().item(1);
            Uni<Number> b = Uni.createFrom().item(2L);
            Uni<Number> c = Uni.createFrom().item(3);

            Uni<List<Number>> uni = Uni.join().all(a, b, c);

            UniAssertSubscriber<List<Number>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, 2L, 3));
        }

        @Test
        void joinDisparateTypesItems() {
            Uni<Object> a = Uni.createFrom().item(1);
            Uni<Object> b = Uni.createFrom().item("2");
            Uni<Object> c = Uni.createFrom().item(3L);

            Uni<List<Object>> uni = Uni.join().all(a, b, c);

            UniAssertSubscriber<List<Object>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertCompleted().assertItem(Arrays.asList(1, "2", 3L));
        }

        @Test
        void joinWithFailedItem() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c);

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        void joinWithFailedItems() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().failure(new IOException("boom"));
            Uni<Integer> c = Uni.createFrom().failure(new IOException("boomz"));

            Uni<List<Integer>> uni = Uni.join().all(a, b, c);

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertFailedWith(IOException.class, "boom");
        }

        @Test
        void earlyCancellation() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().item(2);
            Uni<Integer> c = Uni.createFrom().item(3);

            Uni<List<Integer>> uni = Uni.join().all(a, b, c);
            UniOnItemSpy<List<Integer>> spy = Spy.onItem(uni);

            UniAssertSubscriber<List<Integer>> sub = new UniAssertSubscriber<>(true);
            spy.subscribe().withSubscriber(sub);
            sub.assertNotTerminated();
            assertThat(spy.invocationCount()).isEqualTo(0L);
        }

        @Test
        void lateCancellation() {
            Uni<Integer> a = Uni.createFrom().item(1);
            Uni<Integer> b = Uni.createFrom().emitter(e -> {
                // Do nothing
            });
            Uni<Integer> c = Uni.createFrom().item(3);

            UniOnCancellationSpy<Integer> sa = Spy.onCancellation(a);
            UniOnCancellationSpy<Integer> sb = Spy.onCancellation(b);
            UniOnCancellationSpy<Integer> sc = Spy.onCancellation(c);

            Uni<List<Integer>> uni = Uni.join().all(sa, sb, sc);

            UniAssertSubscriber<List<Integer>> sub = uni.subscribe().withSubscriber(UniAssertSubscriber.create());
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(0L);
            assertThat(sb.invocationCount()).isEqualTo(0L);
            assertThat(sc.invocationCount()).isEqualTo(0L);

            sub.cancel();
            sub.assertNotTerminated();

            assertThat(sa.invocationCount()).isEqualTo(0L);
            assertThat(sb.invocationCount()).isEqualTo(1L);
            assertThat(sc.invocationCount()).isEqualTo(0L);
        }
    }
}
