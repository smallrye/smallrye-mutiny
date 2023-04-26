package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.GroupedMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.TestException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.test.Mocks;
import io.smallrye.mutiny.unchecked.Unchecked;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class MultiGroupTest {

    @AfterEach
    public void cleanup() {
        Infrastructure.resetDroppedExceptionHandler();
    }

    @Test
    public void testGroupIntoListsWithSize0() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoLists().of(0));
    }

    @Test
    public void testGroupIntoListsWithSize0AndSkip() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoLists().of(0, 1));
    }

    @Test
    public void testGroupIntoListsWithSkip0() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoLists().of(1, 0));
    }

    @Test
    public void testGroupIntoListsOfTwoElements() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists().of(2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsDeprecated() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists().of(2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithRequests() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists().of(2)
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber
                .assertSubscribed().assertHasNotReceivedAnyItem()
                .request(3);

        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6));

        subscriber.assertNotTerminated().request(5);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithSkip() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists()
                .of(2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithSkipAndFailure() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists()
                .of(2, 3).onCompletion().fail()
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertFailedWith(NoSuchElementException.class, null);
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithSkipSmallerThanSize() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists()
                .of(2, 1)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(2, 3), Arrays.asList(3, 4),
                Arrays.asList(4, 5), Arrays.asList(5, 6), Arrays.asList(6, 7),
                Arrays.asList(7, 8), Arrays.asList(8, 9), Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithSkipSmallerThanSizeAndFailure() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists()
                .of(2, 1)
                .onCompletion().fail()
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertFailedWith(NoSuchElementException.class, null);
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(2, 3), Arrays.asList(3, 4),
                Arrays.asList(4, 5), Arrays.asList(5, 6), Arrays.asList(6, 7),
                Arrays.asList(7, 8), Arrays.asList(8, 9), Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoListsOfTwoElementsWithRequestsAndSkip() {
        AssertSubscriber<List<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoLists()
                .of(2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber
                .assertSubscribed().assertHasNotReceivedAnyItem()
                .request(2);

        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5));

        subscriber.assertNotTerminated().request(5);

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8));
    }

    @Test
    public void testCombinationOfToListsAndAwait() {
        Multi<List<Integer>> multi = Multi.createFrom().range(1, 10).group().intoLists().of(2);

        assertThat(multi.collect().first().await().indefinitely()).containsExactly(1, 2);
        assertThat(multi.collect().last().await().indefinitely()).containsExactly(9);

        assertThat(multi.collect().asList().await().indefinitely()).hasSize(5)
                .containsExactly(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6),
                        Arrays.asList(7, 8), Collections.singletonList(9));
    }

    @Test
    public void testAsListsOnEmptyStream() {
        assertThat(
                Multi.createFrom().empty().group().intoLists().of(2).collect().last().await().indefinitely())
                .isNull();
    }

    @Test
    public void testAsListsWithNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 10).group().intoLists().every(Duration.ofMillis(-2)));
    }

    @Test
    public void testAsListsWithDuration() {
        MultiOnCancellationSpy<Long> publisher = Spy
                .onCancellation(Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(2))));
        AssertSubscriber<List<Long>> subscriber = publisher.group().intoLists().every(Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(100));

        subscriber.awaitItems(3).cancel();
        publisher.assertCancelled();
    }

    @Test
    public void testAsListsWithDurationWithNoItems() {
        MultiOnCancellationSpy<Long> spy = Spy.onCancellation(Multi.createFrom().<Long> nothing());
        AssertSubscriber<List<Long>> subscriber = spy
                .group().intoLists().every(Duration.ofMillis(100), true)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.awaitItems(3).cancel();
        spy.assertCancelled();
        assertThat(subscriber.getItems()).allSatisfy(l -> assertThat(l).isEmpty());
    }

    @Test
    public void testAsListsWithDurationWithItemsAndNoItems() {
        AssertSubscriber<List<Long>> subscriber = Multi.createFrom().<Long> emitter(Unchecked.consumer(e -> {
            e.emit(1L);
            e.emit(2L);
            e.emit(3L);
            Thread.sleep(200);
            e.emit(4L);
            e.emit(5L);
            Thread.sleep(400);
            e.complete();
        }))
                .group().intoLists().every(Duration.ofMillis(100), true)
                .log()
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.awaitCompletion();
        assertThat(subscriber.getItems()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(subscriber.getItems()).areAtLeastOne(new Condition<>(List::isEmpty, "empty"));
    }

    @Test
    public void testAsListsWithDurationWithCompletion() {
        Multi<Long> publisher = Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(2)))
                .select().first(10);
        AssertSubscriber<List<Long>> subscriber = publisher.group().intoLists().every(Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(100));
        subscriber.awaitCompletion();
    }

    @Test
    public void testAsListsWithDurationWithFailure() {
        Multi<Long> publisher = Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(2)))
                .select().first(10)
                .onCompletion().failWith(new IOException("boom"));
        AssertSubscriber<List<Long>> subscriber = publisher.group().intoLists().every(Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(100));
        subscriber.awaitFailure();
        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @RepeatedTest(5)
    public void testAsListsWithDurationAndLackOfRequests() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<Long> publisher = Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(2)))
                .onCancellation().invoke(() -> cancelled.set(true));
        AssertSubscriber<List<Long>> subscriber = publisher.group().intoLists().every(Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(2));

        subscriber
                .awaitFailure()
                .assertFailedWith(BackPressureFailure.class, "");
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testGroupIntoMultisWithSize0() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoMultis().of(0));
    }

    @Test
    public void testGroupIntoMultisWithSize0AndSkip() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoMultis().of(0, 1));
    }

    @Test
    public void testGroupIntoMultisWithSkip0() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 5).group().intoMultis().of(1, 0));
    }

    @Test
    public void testGroupIntoMultisOfTwoElements() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        List<List<Integer>> flatten = flatten(subscriber.getItems());
        assertThat(flatten).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoMultisOfTwoElementsWithFailure() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2)
                .onCompletion().fail()
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertFailedWith(NoSuchElementException.class, null);
        List<List<Integer>> flatten = flatten(subscriber.getItems());
        assertThat(flatten).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    private <T> List<List<T>> flatten(List<Multi<T>> items) {
        List<List<T>> list = new ArrayList<>();
        for (Multi<T> multi : items) {
            list.add(multi.collect().asList().await().indefinitely());
        }
        return list;
    }

    @Test
    public void testGroupIntoMultisOfTwoElementsWithRequests() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2)
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber
                .assertSubscribed().assertHasNotReceivedAnyItem()
                .request(3);

        assertThat(subscriber.getItems()).hasSize(3);

        subscriber.assertNotTerminated().request(5);

        subscriber.assertCompleted();
        assertThat(flatten(subscriber.getItems())).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6), Arrays.asList(7, 8),
                Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoMultisOfTwoElementsWithSkip() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(flatten(subscriber.getItems())).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8));
    }

    @Test
    public void testGroupIntoMultisOfTwoElementsWithSkipSmallerThanSize() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2, 1)
                .subscribe().withSubscriber(AssertSubscriber.create(100));
        subscriber.assertCompleted();
        assertThat(flatten(subscriber.getItems())).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(2, 3), Arrays.asList(3, 4),
                Arrays.asList(4, 5), Arrays.asList(5, 6), Arrays.asList(6, 7),
                Arrays.asList(7, 8), Arrays.asList(8, 9), Collections.singletonList(9));
    }

    @Test
    public void testGroupIntoMultisOfTwoElementsWithRequestsAndSkip() {
        AssertSubscriber<Multi<Integer>> subscriber = Multi.createFrom().range(1, 10).group().intoMultis()
                .of(2, 3)
                .subscribe().withSubscriber(AssertSubscriber.create());
        subscriber
                .assertSubscribed().assertHasNotReceivedAnyItem()
                .request(2);

        assertThat(subscriber.getItems()).hasSize(2);

        subscriber.assertNotTerminated().request(5);

        subscriber.assertCompleted();
        assertThat(flatten(subscriber.getItems())).containsExactly(
                Arrays.asList(1, 2), Arrays.asList(4, 5), Arrays.asList(7, 8));
    }

    @Test
    public void testCombinationOfToMultisAndAwait() {
        Multi<Multi<Integer>> multi = Multi.createFrom().range(1, 10).group().intoMultis().of(2);

        assertThat(multi.collect().first().await().indefinitely().collect().asList().await().indefinitely())
                .containsExactly(1, 2);
        assertThat(multi.collect().last().await().indefinitely().collect().asList().await().indefinitely())
                .containsExactly(9);

        assertThat(flatten(multi.collect().asList().await().indefinitely())).hasSize(5)
                .containsExactly(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6),
                        Arrays.asList(7, 8), Collections.singletonList(9));
    }

    @Test
    public void testAsMultisOnEmptyStream() {
        assertThat(
                Multi.createFrom().empty().group().intoMultis().of(2).collect().last().await().indefinitely())
                .isNull();
    }

    @Test
    public void testAsMultisWithNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 10).group().intoMultis().every(Duration.ofMillis(-2)));
    }

    @Test
    public void testAsMultisWithDuration() {
        Multi<Long> publisher = Multi.createFrom().publisher(Multi.createFrom().ticks().every(Duration.ofMillis(2)));
        AssertSubscriber<Multi<Long>> subscriber = publisher.group().intoMultis()
                .every(Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(100));

        subscriber.awaitItems(3).cancel();
    }

    @Test
    public void testBasicTimeWindow() {
        Multi<Multi<Integer>> multi = Multi.createFrom().range(1, 7)
                .group().intoMultis().every(Duration.ofMillis(1));
        Uni<List<Integer>> uni = multi
                .onItem().transformToMultiAndConcatenate(m -> m)
                .collect().asList();

        List<Integer> list = uni.await().atMost(Duration.ofSeconds(4));
        assertThat(list).contains(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testBasicTimeWindowWithFailure() {
        Multi<Integer> multi = Multi.createBy().concatenating().streams(
                Multi.createFrom().range(1, 7),
                Multi.createFrom().failure(() -> new IOException("boom")))
                .group().intoMultis().every(Duration.ofMillis(1))
                .onItem().transformToMultiAndConcatenate(m -> m);

        multi.subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE))
                .awaitFailure()
                .assertItems(1, 2, 3, 4, 5, 6)
                .assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testThatWindowWithDurationEmitsEmptyLists() {
        AssertSubscriber<List<Object>> subscriber = AssertSubscriber.create(3);
        Multi.createFrom().nothing()
                .group().intoMultis().every(Duration.ofMillis(10))
                .onItem().transformToUniAndMerge(m -> m.collect().asList())
                .subscribe().withSubscriber(subscriber);

        subscriber.awaitItems(3).cancel();
        List<List<Object>> items = subscriber.getItems();
        assertThat(items).hasSize(3).allSatisfy(list -> assertThat(list).isEmpty());
    }

    @Test
    public void testGroupByWithKeyMapperOnly() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().range(1, 10)
                .group().by(i -> i % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).hasSize(2);

        List<Integer> odd = subscriber.getItems().get(0).collect().asList().await().indefinitely();
        List<Integer> even = subscriber.getItems().get(1).collect().asList().await().indefinitely();

        assertThat(subscriber.getItems().get(0).key()).isEqualTo(1);
        assertThat(subscriber.getItems().get(1).key()).isEqualTo(0);

        assertThat(odd).containsExactly(1, 3, 5, 7, 9);
        assertThat(even).containsExactly(2, 4, 6, 8);
    }

    @Test
    public void testGroupByWithKeyMapperAndValueMapper() {
        AssertSubscriber<GroupedMulti<Integer, String>> subscriber = Multi.createFrom().range(1, 10)
                .group().by(i -> i % 2, t -> Integer.toString(t))
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).hasSize(2);

        List<String> odd = subscriber.getItems().get(0).collect().asList().await().indefinitely();
        List<String> even = subscriber.getItems().get(1).collect().asList().await().indefinitely();

        assertThat(subscriber.getItems().get(0).key()).isEqualTo(1);
        assertThat(subscriber.getItems().get(1).key()).isEqualTo(0);

        assertThat(odd).containsExactly("1", "3", "5", "7", "9");
        assertThat(even).containsExactly("2", "4", "6", "8");
    }

    @Test
    public void testGroupByProducingASingleGroup() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().range(1, 10)
                .group().by(i -> 0)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).hasSize(1);
        List<Integer> numbers = subscriber.getItems().get(0).collect().asList().await().indefinitely();
        assertThat(subscriber.getItems().get(0).key()).isEqualTo(0);
        assertThat(numbers).hasSize(9);
    }

    @Test
    public void testGroupByWithNullKeyMapper() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().range(1, 10).group().by(null));
    }

    @Test
    public void testGroupByWithNullValueMapper() {
        assertThrows(IllegalArgumentException.class,
                () -> Multi.createFrom().range(1, 10).group().by(i -> i % 2, null));
    }

    @Test
    public void testGroupByOnFailingMulti() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom()
                .<Integer> failure(new IOException("boom"))
                .group().by(i -> i % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertFailedWith(IOException.class, "boom");
        assertThat(subscriber.getItems()).hasSize(0);
    }

    @Test
    public void testGroupByOnEmptyMulti() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().<Integer> empty()
                .group().by(i -> i % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testGroupByFollowedWithAFlatMap() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom().range(1, 10)
                .group().by(i -> 1)
                .flatMap(gm -> gm)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertCompleted();
        assertThat(subscriber.getItems()).hasSize(9);
    }

    @Test
    public void requestingIsResumedAfterCancellationOfAGroupedMulti() {
        final List<AssertSubscriber<Integer>> subscribers = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean done = new AtomicBoolean();
        Multi.createFrom().range(0, 1001)
                .onItem().invoke(x -> counter.getAndIncrement())
                .group().intoMultis().of(1)
                .subscribe().with(multi -> {
                    AssertSubscriber<Integer> subscriber = AssertSubscriber.create(0L);
                    subscribers.add(subscriber);
                    multi.subscribe(subscriber);
                },
                        () -> done.set(true));

        while (!done.get()) {
            subscribers.remove(0).cancel();
        }

        assertThat(counter.get()).isEqualTo(1001);
    }

    @Test
    public void testSkipGroupWithFailures() {
        AssertSubscriber<List<Object>> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .group().intoLists().of(2, 4)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testSkipGroupAsMultisWithFailures() {
        AssertSubscriber<Multi<Object>> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .group().intoMultis().of(2, 4)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOverlapGroupWithFailures() {
        AssertSubscriber<List<Object>> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .group().intoLists().of(4, 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testOverlapGroupAsMultisWithFailures() {
        AssertSubscriber<Multi<Object>> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .group().intoMultis().of(4, 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testExactGroupAsMultisWithFailures() {
        AssertSubscriber<Multi<Object>> subscriber = Multi.createFrom().failure(new IOException("boom"))
                .group().intoMultis().of(2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom");
    }

    @Test
    public void testSkipGroupWithRogueFailures() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(caught::set);

        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onItem(4);
                subscriber.onFailure(new IOException("boom-1"));
                subscriber.onItem(5);
                subscriber.onItem(6);
                subscriber.onFailure(new IOException("boom-2"));
                subscriber.onCompletion();
            }
        };

        AssertSubscriber<List<Integer>> subscriber = rogue
                .group().intoLists().of(2, 4)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom-1");
        assertThat(caught.get()).hasMessageContaining("boom-2");
    }

    @Test
    public void testSkipGroupAsMultisWithRogueFailures() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(caught::set);

        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onItem(4);
                subscriber.onFailure(new IOException("boom-1"));
                subscriber.onItem(5);
                subscriber.onItem(6);
                subscriber.onFailure(new IOException("boom-2"));
                subscriber.onCompletion();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .group().intoMultis().of(2, 4)
                .onItem().transformToMultiAndConcatenate(m -> m)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom-1");
        assertThat(caught.get()).hasMessageContaining("boom-2");
    }

    @Test
    public void testOverlapGroupWithRogueFailures() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(caught::set);

        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onItem(4);
                subscriber.onFailure(new IOException("boom-1"));
                subscriber.onItem(5);
                subscriber.onItem(6);
                subscriber.onFailure(new IOException("boom-2"));
                subscriber.onCompletion();
            }
        };

        AssertSubscriber<List<Integer>> subscriber = rogue
                .group().intoLists().of(4, 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom-1");
        assertThat(caught.get()).hasMessageContaining("boom-2");
    }

    @Test
    public void testOverlapGroupAsMultisWithRogueFailures() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(caught::set);

        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onItem(4);
                subscriber.onFailure(new IOException("boom-1"));
                subscriber.onItem(5);
                subscriber.onItem(6);
                subscriber.onFailure(new IOException("boom-2"));
                subscriber.onCompletion();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .group().intoMultis().of(4, 2)
                .onItem().transformToMultiAndConcatenate(m -> m)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom-1");
        assertThat(caught.get()).hasMessageContaining("boom-2");
    }

    @Test
    public void testExactGroupAsMultisWithRogueFailures() {
        AtomicReference<Throwable> caught = new AtomicReference<>();
        Infrastructure.setDroppedExceptionHandler(caught::set);

        AbstractMulti<Integer> rogue = new AbstractMulti<Integer>() {
            @Override
            public void subscribe(MultiSubscriber<? super Integer> subscriber) {
                subscriber.onSubscribe(mock(Subscription.class));
                subscriber.onItem(1);
                subscriber.onItem(2);
                subscriber.onItem(3);
                subscriber.onItem(4);
                subscriber.onFailure(new IOException("boom-1"));
                subscriber.onItem(5);
                subscriber.onItem(6);
                subscriber.onFailure(new IOException("boom-2"));
                subscriber.onCompletion();
            }
        };

        AssertSubscriber<Integer> subscriber = rogue
                .group().intoMultis().of(2)
                .onItem().transformToMultiAndConcatenate(m -> m)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertFailedWith(IOException.class, "boom-1");
        assertThat(caught.get()).hasMessageContaining("boom-2");
    }

    @Test
    public void testInvalidRequestsWhenGroupingIntoLists() {
        Subscriber<List<Integer>> listExact = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoLists().of(2)
                .subscribe().withSubscriber(listExact);
        verify(listExact).onError(any(IllegalArgumentException.class));

        Subscriber<List<Integer>> listSkip = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoLists().of(2, 4)
                .subscribe().withSubscriber(listSkip);
        verify(listSkip).onError(any(IllegalArgumentException.class));

        Subscriber<List<Integer>> listOverlap = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoLists().of(4, 2)
                .subscribe().withSubscriber(listOverlap);
        verify(listOverlap).onError(any(IllegalArgumentException.class));
    }

    @Test
    public void testInvalidRequestsWhenGroupingIntoMultis() {
        Subscriber<Multi<Integer>> listExact = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoMultis().of(2)
                .subscribe().withSubscriber(listExact);
        verify(listExact).onError(any(IllegalArgumentException.class));

        Subscriber<Multi<Integer>> listSkip = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoMultis().of(2, 4)
                .subscribe().withSubscriber(listSkip);
        verify(listSkip).onError(any(IllegalArgumentException.class));

        Subscriber<Multi<Integer>> listOverlap = Mocks.subscriber(-1);
        Multi.createFrom().items(1, 2, 3, 4)
                .group().intoMultis().of(4, 2)
                .subscribe().withSubscriber(listOverlap);
        verify(listOverlap).onError(any(IllegalArgumentException.class));

    }

    @Test
    public void testGroupByWithKeyMapperThrowingException() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().range(1, 10)
                .group().<Integer> by(i -> {
                    throw new ArithmeticException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertFailedWith(ArithmeticException.class, "boom");
    }

    @Test
    public void testGroupByWithValueMapperThrowingException() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().range(1, 10)
                .group().<Integer, Integer> by(i -> i % 2, i -> {
                    throw new TestException("boom");
                })
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testGroupByWithValueMapperReturningNull() {
        AssertSubscriber<GroupedMulti<Integer, Integer>> subscriber = Multi.createFrom().range(1, 10)
                .group().<Integer, Integer> by(i -> i % 2, i -> null)
                .subscribe().withSubscriber(AssertSubscriber.create(100));

        subscriber.assertFailedWith(NullPointerException.class, "");
    }

    @Test
    public void testCancellationOnUpstream() {
        AssertSubscriber<GroupedMulti<Long, Long>> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .onOverflow().drop()
                .group().by(l -> l % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.awaitSubscription().awaitItems(2).cancel();

        AssertSubscriber<Long> s1 = subscriber.getItems().get(0).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s1.assertSubscribed();
        AssertSubscriber<Long> s2 = subscriber.getItems().get(1).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s2.assertSubscribed();

        await().until(() -> s1.getItems().size() >= 3);
        await().until(() -> s2.getItems().size() >= 3);

        subscriber.cancel();

        assertThat(subscriber.getItems()).hasSize(2);
        s1.assertNotTerminated();
        s2.assertNotTerminated();

    }

    @Test
    public void testCancellationOnGroup() {
        AssertSubscriber<GroupedMulti<Long, Long>> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .group().by(l -> l % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertSubscribed().awaitItems(2, Duration.ofSeconds(1));
        AssertSubscriber<Long> s1 = subscriber.getItems().get(0).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s1.assertSubscribed();
        AssertSubscriber<Long> s2 = subscriber.getItems().get(1).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s2.assertSubscribed();

        await().until(() -> s1.getItems().size() >= 3);
        await().until(() -> s2.getItems().size() >= 3);

        s2.cancel();

        await().until(() -> s1.getItems().size() >= 5);

        subscriber.cancel();
    }

    @Test
    public void testImmediateCancellationOnUpstream() {
        AssertSubscriber<GroupedMulti<Long, Long>> subscriber = Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .group().by(l -> l % 2)
                .subscribe().withSubscriber(new AssertSubscriber<>(Long.MAX_VALUE, true));

        subscriber.assertSubscribed();
        assertThat(subscriber.assertNotTerminated().isCancelled()).isTrue();
    }

    @RepeatedTest(10)
    public void testGroupByWithUpstreamFailure() {
        AtomicReference<MultiEmitter<? super Long>> emitter = new AtomicReference<>();

        Multi<Long> multi = Multi.createBy().merging().streams(
                Multi.createFrom().ticks().every(Duration.ofMillis(2)),
                Multi.createFrom().emitter((Consumer<MultiEmitter<? super Long>>) emitter::set));

        AssertSubscriber<GroupedMulti<Long, Long>> subscriber = multi
                .group().by(l -> l % 2)
                .subscribe().withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));

        subscriber.assertSubscribed();
        await().until(() -> subscriber.getItems().size() == 2);
        AssertSubscriber<Long> s1 = subscriber.getItems().get(0).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s1.assertSubscribed();
        AssertSubscriber<Long> s2 = subscriber.getItems().get(1).subscribe()
                .withSubscriber(AssertSubscriber.create(Long.MAX_VALUE));
        s2.assertSubscribed();

        await().until(() -> s1.getItems().size() >= 3);
        await().until(() -> s2.getItems().size() >= 3);

        emitter.get().fail(new TestException("boom"));

        s1
                .awaitFailure()
                .assertFailedWith(TestException.class, "boom");
        s2
                .awaitFailure()
                .assertFailedWith(TestException.class, "boom");
        subscriber.assertFailedWith(TestException.class, "boom");
    }

    @Test
    public void testGroupIntoListsWithMaximumDelay() {
        Multi<Long> publisher = Multi.createFrom().ticks().every(Duration.ofMillis(10));

        AssertSubscriber<List<Long>> subscriber = publisher.group().intoLists().of(3, Duration.ofMillis(100))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(10));

        await().timeout(Duration.ofSeconds(2)).until(() -> subscriber.getItems().size() >= 9);
        subscriber.cancel();

        List<List<Long>> batches = subscriber.getItems();
        assertThat(batches.size()).isGreaterThanOrEqualTo(9);

        for (List<Long> batch : batches) {
            assertThat(batch.size()).isBetween(1, 3);
        }
    }
}
