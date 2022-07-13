package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Flowable;
import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.spies.MultiOnCancellationSpy;
import io.smallrye.mutiny.helpers.spies.Spy;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.flow.adapters.AdaptersToFlow;

public class MultiConcatTest {

    @Test
    public void testConcatenatingNothing() {
        AssertSubscriber<?> subscriber = Multi.createBy().concatenating().streams()
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testConcatenatingOne() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(Multi.createFrom().items(1, 2, 3))
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(1, 2, 3);
    }

    @Test
    public void testConcatenatingOneButEmpty() {
        AssertSubscriber<?> subscriber = Multi.createBy().concatenating().streams(Multi.createFrom().empty())
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertHasNotReceivedAnyItem();
    }

    @Test
    public void testConcatenatingOneButNever() {
        MultiOnCancellationSpy<Object> spy = Spy.onCancellation(Multi.createFrom().nothing());
        AssertSubscriber<?> subscriber = Multi.createBy().concatenating().streams(spy)
                .subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertNotTerminated()
                .cancel();

        assertThat(spy.isCancelled()).isTrue();
    }

    @Test
    public void testConcatenationOfSeveralMultis() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Multi.createFrom().item(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralMultisWithConcurrency() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating()
                .streams(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralMultisAsIterable() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Arrays.asList(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralPublishers() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                AdaptersToFlow.publisher(Flowable.just(5)),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)).subscribe()
                .withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenationOfSeveralPublishersAsIterable() {
        AssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().streams(
                Arrays.asList(
                        AdaptersToFlow.publisher(Flowable.just(5)),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().transform(i -> i + 1)))
                .subscribe().withSubscriber(new AssertSubscriber<>(100));

        subscriber.assertCompleted()
                .assertItems(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testConcatenatingWithEmpty() {
        Multi.createBy().concatenating().streams(Multi.createFrom().empty(), Multi.createFrom().item(2))
                .subscribe().withSubscriber(AssertSubscriber.create(1))
                .assertCompleted().assertItems(2);
    }

    @Test
    public void testWithFailureCollection() {
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
}
