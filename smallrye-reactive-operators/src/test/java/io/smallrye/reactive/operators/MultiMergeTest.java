package io.smallrye.reactive.operators;

import io.reactivex.Flowable;
import io.smallrye.reactive.CompositeException;
import io.smallrye.reactive.Multi;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiMergeTest {

    @Test
    public void testMergeOfSeveralMultis() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().merging().multis(
                Multi.createFrom().item(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().mapToItem(i -> i + 1)
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralMultisWithConcurrencyAndRequests() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().merging().withConcurrency(2).withRequests(1)
                .multis(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().mapToItem(i -> i + 1)
                ).subscribe().withSubscriber(new MultiAssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralMultisAsIterable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().merging().multis(
                Arrays.asList(
                        Multi.createFrom().item(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().mapToItem(i -> i + 1)
                )
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralPublishers() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().merging().publishers(
                Flowable.just(5),
                Multi.createFrom().range(1, 3),
                Multi.createFrom().items(8, 9, 10).onItem().mapToItem(i -> i + 1)
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergeOfSeveralPublishersAsIterable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().merging().publishers(
                Arrays.asList(
                        Flowable.just(5),
                        Multi.createFrom().range(1, 3),
                        Multi.createFrom().items(8, 9, 10).onItem().mapToItem(i -> i + 1)
                )
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(100));

        subscriber.assertCompletedSuccessfully()
                .assertReceived(5, 1, 2, 9, 10, 11);
    }

    @Test
    public void testMergingEmpty() {
        Multi.createBy().merging().multis(Multi.createFrom().empty())
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testMergingWithEmpty() {
        Multi.createBy().merging().multis(Multi.createFrom().empty(), Multi.createFrom().item(2))
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully().assertReceived(2);
    }

    @Test
    public void testWithFailureCollection() {
        IllegalStateException boom = new IllegalStateException("boom");
        IllegalStateException boom2 = new IllegalStateException("boom2");

        MultiAssertSubscriber<Integer> subscriber = Multi.createBy().concatenating().collectFailures().publishers(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom2)
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertReceived(5, 6)
                .assertHasFailedWith(CompositeException.class, "boom")
                .assertHasFailedWith(CompositeException.class, "boom2");

        assertThat(subscriber.failures().get(0)).isInstanceOf(CompositeException.class);
        CompositeException ce = (CompositeException) subscriber.failures().get(0);
        assertThat(ce.getCauses()).hasSize(2);

        subscriber = Multi.createBy().concatenating().publishers(
                Multi.createFrom().item(5),
                Multi.createFrom().failure(boom),
                Multi.createFrom().item(6),
                Multi.createFrom().failure(boom)
        ).subscribe().withSubscriber(new MultiAssertSubscriber<>(5));

        subscriber.assertTerminated()
                .assertReceived(5)
                .assertHasFailedWith(IllegalStateException.class, "boom");

    }
}
