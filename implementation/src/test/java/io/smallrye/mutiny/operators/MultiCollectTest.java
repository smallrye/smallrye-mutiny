package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class MultiCollectTest {

    private final Multi<Person> persons = Multi.createFrom().items(
            new Person("bob", 1),
            new Person("alice", 2),
            new Person("rob", 3),
            new Person("matt", 4));
    private final Multi<Person> personsWithDuplicates = Multi.createFrom().items(
            new Person("bob", 1),
            new Person("alice", 2),
            new Person("rob", 3),
            new Person("matt", 4),
            new Person("bob", 5),
            new Person("rob", 6));

    @Test
    public void testCollectFirstAndLast() {
        Multi<Integer> items = Multi.createFrom().items(1, 2, 3);
        items
                .collect().first()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(1);

        items
                .collect().last()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(3);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testCollectFirstAndLastDeprecated() {
        Multi<Integer> items = Multi.createFrom().items(1, 2, 3);
        items
                .collectItems().first()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(1);

        items
                .collectItems().last()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(3);
    }

    @Test
    public void testCollectWithEmpty() {
        Multi<Integer> items = Multi.createFrom().empty();
        items
                .collect().first()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(null);

        items
                .collect().last()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertItem(null);
    }

    @Test
    public void testCollectFirstAndLastOnFailure() {
        Multi<Integer> failing = Multi.createFrom().failure(new IOException("boom"));
        failing
                .collect().first()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertFailedWith(IOException.class, "boom");

        failing
                .collect().last()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await()
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testAsList() {
        UniAssertSubscriber<List<Integer>> subscriber = Multi.createFrom().items(1, 2, 3)
                .collect().asList()
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await();

        assertThat(subscriber.getItem()).containsExactly(1, 2, 3);
    }

    @Test
    public void testCollectIn() {
        UniAssertSubscriber<LinkedList<Integer>> subscriber = Multi.createFrom().range(1, 10)
                .collect().in(LinkedList<Integer>::new, LinkedList::add)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .await();

        assertThat(subscriber.getItem()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).isInstanceOf(LinkedList.class);
    }

    @Test
    public void testCollectInWithSupplierThrowingException() {
        Multi.createFrom().range(1, 10)
                .collect().in(() -> {
                    throw new IllegalArgumentException("boom");
                }, (x, y) -> {
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testCollectInWithAccumulatorThrowingException() {
        Multi.createFrom().range(1, 10)
                .collect().in(LinkedList<Integer>::new, (list, res) -> {
                    list.add(res);
                    if (res == 5) {
                        throw new IllegalArgumentException("boom");
                    }
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(IllegalArgumentException.class, "boom");
    }

    @Test
    public void testCollectInWithSupplierReturningNull() {
        Multi.createFrom().range(1, 10)
                .collect().in(() -> null, (x, y) -> {
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NullPointerException.class, "supplier");
    }

    @Test
    public void testCollectInWithAccumulatorSupplierReturningNull() {
        Multi.createFrom().range(1, 10)
                .collect().with(new Collector<Integer, Integer, Integer>() {
                    @Override
                    public Supplier<Integer> supplier() {
                        return () -> 0;
                    }

                    @Override
                    public BiConsumer<Integer, Integer> accumulator() {
                        return null;
                    }

                    @Override
                    public BinaryOperator<Integer> combiner() {
                        return (a, b) -> 0;
                    }

                    @Override
                    public Function<Integer, Integer> finisher() {
                        return i -> i;
                    }

                    @Override
                    public Set<Characteristics> characteristics() {
                        return Collections.emptySet();
                    }
                })
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertFailedWith(NullPointerException.class, "accumulator");
    }

    @Test
    public void testCollectIntoMap() {
        UniAssertSubscriber<Map<String, Person>> subscriber = persons
                .collect().asMap(p -> p.firstName)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        assertThat(subscriber.getItem())
                .hasSize(4)
                .contains(
                        entry("bob", new Person("bob", 1)),
                        entry("alice", new Person("alice", 2)),
                        entry("rob", new Person("rob", 3)),
                        entry("matt", new Person("matt", 4)));
    }

    @Test
    public void testCollectIntoMapWithKeyAndValueMappers() {
        UniAssertSubscriber<Map<String, String>> subscriber = persons
                .collect().asMap(p -> p.firstName, v -> v.firstName.toUpperCase())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        assertThat(subscriber.getItem())
                .hasSize(4)
                .contains(
                        entry("bob", "BOB"),
                        entry("alice", "ALICE"),
                        entry("rob", "ROB"),
                        entry("matt", "MATT"));
    }

    @Test
    public void testCollectAsMapWithEmpty() {
        UniAssertSubscriber<Map<String, Person>> subscriber = Multi.createFrom().<Person> empty()
                .collect().asMap(p -> p.firstName)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .assertCompleted();

        assertThat(subscriber.getItem()).isEmpty();
    }

    @Test
    public void testCollectAsMultiMap() {
        UniAssertSubscriber<Map<String, Collection<Person>>> subscriber = personsWithDuplicates
                .collect().asMultiMap(p -> p.firstName)
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .assertCompleted();

        assertThat(subscriber.getItem()).hasSize(4);
        assertThat(subscriber.getItem().get("alice")).containsExactly(new Person("alice", 2));
        assertThat(subscriber.getItem().get("rob")).hasSize(2)
                .contains(new Person("rob", 3), new Person("rob", 6));

    }

    @Test
    public void testCollectAsMultiMapWithValueMapper() {
        UniAssertSubscriber<Map<String, Collection<Long>>> subscriber = personsWithDuplicates
                .collect().asMultiMap(p -> p.firstName, p -> p.id)
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .assertCompleted();

        assertThat(subscriber.getItem()).hasSize(4);
        assertThat(subscriber.getItem().get("alice")).containsExactly(2L);
        assertThat(subscriber.getItem().get("rob")).hasSize(2)
                .contains(3L, 6L);
    }

    @Test
    public void testCollectAsMultiMapOnEmpty() {
        UniAssertSubscriber<Map<String, Collection<Person>>> subscriber = Multi.createFrom().<Person> empty()
                .collect().asMultiMap(p -> p.firstName)
                .subscribe().withSubscriber(new UniAssertSubscriber<>())
                .assertCompleted();
        assertThat(subscriber.getItem()).hasSize(0);

    }

    @Test
    public void testSumCollector() {
        Multi.createFrom().range(1, 5).collect().with(Collectors.summingInt(value -> value))
                .subscribe().withSubscriber(new UniAssertSubscriber<>()).assertCompleted().assertItem(10);
    }

    @Test
    public void testWithFinisherReturningNull() {
        List<String> list = new ArrayList<>();
        Multi.createFrom().items("a", "b", "c")
                .map(String::toUpperCase)
                .collect().with(Collector.of(() -> null, (n, t) -> list.add(t), (X, y) -> null, x -> null))
                .await().indefinitely();
        assertThat(list).containsExactly("A", "B", "C");
    }

    @Test
    public void testCollectWhere() {
        Uni<List<Integer>> uni = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .collect().where(i -> i % 2 == 0)
                .asList();

        List<Integer> integers = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(integers).containsExactly(2, 4, 6, 8, 10);
    }

    @Test
    public void testCollectWhereEmpty() {
        Uni<List<Integer>> uni = Multi.createFrom().<Integer> empty()
                .collect().where(i -> i % 2 == 0)
                .asList();

        List<Integer> integers = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(integers).isEmpty();
    }

    @Test
    public void testCollectWhereWithException() {
        Uni<List<Integer>> uni = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .collect().where(i -> {
                    if (i == 3) {
                        throw new IllegalArgumentException("boom");
                    }
                    return i % 2 == 0;
                })
                .asList();

        assertThatThrownBy(() -> uni.await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boom");
    }

    @Test
    public void testCollectWhen() {
        Uni<List<Integer>> uni = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .collect().when(i -> Uni.createFrom().item(i % 2 == 0))
                .asList();

        List<Integer> integers = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(integers).containsExactly(2, 4, 6, 8, 10);
    }

    @Test
    public void testCollectWhenEmpty() {
        Uni<List<Integer>> uni = Multi.createFrom().<Integer> empty()
                .collect().when(i -> Uni.createFrom().item(i % 2 == 0))
                .asList();

        List<Integer> integers = uni.await().atMost(Duration.ofSeconds(1));
        assertThat(integers).isEmpty();
    }

    @Test
    public void testCollectWhenWithException() {
        Uni<List<Integer>> uni = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .collect().when(i -> {
                    if (i == 3) {
                        throw new IllegalArgumentException("boom");
                    }
                    return Uni.createFrom().item(i % 2 == 0);
                })
                .asList();

        assertThatThrownBy(() -> uni.await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boom");
    }

    @Test
    public void testCollectWhenWithFailure() {
        Uni<List<Integer>> uni = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .collect().when(i -> Uni.createFrom().<Boolean> emitter(e -> {
                    if (i == 3) {
                        e.fail(new IllegalArgumentException("boom"));
                    }
                    e.complete(i % 2 == 0);
                }))
                .asList();

        assertThatThrownBy(() -> uni.await().atMost(Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boom");
    }

    static class Person {

        private final String firstName;

        private final long id;

        Person(String firstName, long id) {
            this.firstName = firstName;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Person person = (Person) o;
            return id == person.id &&
                    firstName.equals(person.firstName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(firstName, id);
        }

    }

}
