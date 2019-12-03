package io.smallrye.mutiny.converters;

import java.io.IOException;

import org.junit.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.RxConverters;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromACompletable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(RxConverters.fromCompletable(), Completable.complete())
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(RxConverters.fromCompletable(), Completable.error(new IOException("boom")))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromASingle() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromSingle(), Single.just(1))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromSingle(), Single.<Integer> error(new IOException("boom")))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAMaybe() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromMaybe(), Maybe.just(1))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(RxConverters.fromMaybe(), Maybe.<Void> empty())
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromMaybe(), Maybe.<Integer> error(new IOException("boom")))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAFlowable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromFlowable(), Flowable.just(1))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromFlowable(), Flowable.just(1, 2, 3))
                .subscribe()
                .with(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(RxConverters.fromFlowable(), Flowable.<Void> empty())
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromFlowable(), Flowable.<Integer> error(new IOException("boom")))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }

    @Test
    public void testCreatingFromAnObserver() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromObservable(), Observable.just(1))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertReceived(1);
    }

    @Test
    public void testCreatingFromAMultiValuedObservable() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromObservable(), Observable.just(1, 2, 3))
                .subscribe()
                .with(MultiAssertSubscriber.create(3));

        subscriber.assertCompletedSuccessfully().assertReceived(1, 2, 3);
    }

    @Test
    public void testCreatingFromAnEmptyObservable() {
        MultiAssertSubscriber<Void> subscriber = Multi.createFrom()
                .converter(RxConverters.fromObservable(), Observable.<Void> empty())
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertCompletedSuccessfully().assertHasNotReceivedAnyItem();
    }

    @Test
    public void testCreatingFromAnObservableWithFailure() {
        MultiAssertSubscriber<Integer> subscriber = Multi.createFrom()
                .converter(RxConverters.fromObservable(), Observable.<Integer> error(new IOException("boom")))
                .subscribe()
                .with(MultiAssertSubscriber.create(1));

        subscriber.assertHasFailedWith(IOException.class, "boom");
    }
}
