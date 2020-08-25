package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import io.smallrye.mutiny.test.AssertSubscriber;

@SuppressWarnings("ConstantConditions")
public class MultiConvertFromTest {

    @Test
    public void testCreatingFromACompletable() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), Completable.complete())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), Completable.error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromASingle() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromSingle(), Single.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromSingle(), Single.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAMaybe() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMaybeNeverEmitting() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Multi<Integer> multi = Multi.createFrom().converter(MultiRxConverters.fromMaybe(), Maybe.<Integer> never()
                .doOnDispose(() -> cancelled.set(true)));
        assertThat(multi).isNotNull();
        multi.subscribe().with(i -> {
        }).cancel();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.<Void> empty())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), Maybe.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlowable() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.just(1, 2, 3))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.<Void> empty())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromFlowable(), Flowable.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAnObserver() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.just(1))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedObservable() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.just(1, 2, 3))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyObservable() {
        AssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.<Void> empty())
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAnObservableWithFailure() {
        AssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), Observable.<Integer> error(new IOException("boom")))
                .subscribe()
                .withSubscriber(AssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }
}
