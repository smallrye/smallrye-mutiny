package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class MultiMergeTest {

    @Test
    public void testMergeNothing() {
        AssertSubscriber<?> subscriber = Multi.createBy().merging().streams()
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergeOne() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().streams(Multi.createFrom().items(1, 2, 3))
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testMergeOneButEmpty() {
        AssertSubscriber<?> subscriber = Multi.createBy().merging().streams(Multi.createFrom().empty())
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergeOneButNever() {
        MultiOnCancellationSpy<Object> spy = Spy.onCancellation(Multi.createFrom().nothing());
        AssertSubscriber<?> subscriber = Multi.createBy().merging().streams(spy)
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertNotTerminated()
                .cancel();

        assertThat(spy.isCancelled()).isTrue();
    }

    @Test
    public void testMergeOfSeveralMultis() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralMultisWithConcurrencyAndRequests() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().withConcurrency(2).withRequests(1)
                .streams(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralMultisAsIterable() {
        final List<Multi<Integer>> multis = Arrays.asList(
                Multi.createFrom().item(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1));
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().streams(multis)
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralPublishers() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().streams(
                AdaptersToFlow.publisher(Flowable.just(5)),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralPublishersAsIterable() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().streams(
                Arrays.asList(
                        AdaptersToFlow.publisher(Flowable.just(5)),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergingEmpty() {
        Multi.createBy().merging().streams(Multi.createFrom().empty())
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergingWithEmpty() {
        Multi.createBy().merging().streams(Multi.createFrom().empty(), Multi.createFrom().item(2))
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted().assertItems(2);
    }

    @Test
    public void testWithFailureCollectionWithConcatenation() {
        IllegalStateException boom = new IllegalStateException("boom");
        IllegalStateException boom2 = new IllegalStateException("boom2");

        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().collectFailures().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom2)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertItems(5, 6)
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "boom2");

        assertThat(subscriber.getFailure()).isInstanceOf(CompositeException.class);
        CompositeException ce = (CompositeException) subscriber.getFailure();
        assertThat(ce.getCauses()).hasSize(2);

        subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertItems(5)
                .assertFailedWith(IllegalStateException.class, "boom");

    }

    @Test
    public void testWithFailureCollectionWithMerge() {
        IllegalStateException boom = new IllegalStateException("boom");
        IllegalStateException boom2 = new IllegalStateException("boom2");

        AssertSubscriber<Integer> subscriber = Multi.createBy().merging().collectFailures().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom2)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertItems(5, 6)
                .assertFailedWith(CompositeException.class, "boom")
                .assertFailedWith(CompositeException.class, "boom2");

        assertThat(subscriber.getFailure()).isInstanceOf(CompositeException.class);
        CompositeException ce = (CompositeException) subscriber.getFailure();
        assertThat(ce.getCauses()).hasSize(2);

        subscriber = Multi.createBy().merging().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom)).subscribe().withSubscriber(new AssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertItems(5)
                .assertFailedWith(IllegalStateException.class, "boom");

    }
}
