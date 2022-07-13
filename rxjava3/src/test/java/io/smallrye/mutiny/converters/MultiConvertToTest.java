package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiRx3Converters;
import io.smallrye.mutiny.subscription.MultiEmitter;

@SuppressWarnings("ConstantConditions")
public class MultiConvertToTest {

    @Test
    public void testCreatingACompletable() {
        Completable completable = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testThatSubscriptionOnCompletableProducesTheValue() {
        AtomicBoolean called = new AtomicBoolean();
        Completable completable = Multi.createFrom().deferred(() -> {
            called.set(true);
            return Multi.createFrom().item(2);
        }).convert().with(MultiRx3Converters.toCompletable());

        assertThat(completable).isNotNull();
        assertThat(called).isFalse();
        completable.test().assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingACompletableFromVoid() {
        Completable completable = Multi.createFrom().item((Object) null).convert()
                .with(MultiRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testCreatingACompletableWithFailure() {
        Completable completable = Multi.createFrom().failure(new IOException("boom")).convert()
                .with(MultiRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingASingle() {
        Single<Optional<Integer>> single = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleByConverter() {
        Single<Optional<Integer>> single = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleFromNull() {
        Single<Integer> single = Multi.createFrom().item((Integer) null).convert()
                .with(MultiRx3Converters.toSingle().onEmptyThrow(() -> new NoSuchElementException("not found")));
        assertThat(single).isNotNull();
        single
                .test()
                .assertFailure(NoSuchElementException.class)
                .assertNoValues();
    }

    @Test
    public void testCreatingASingleFromNullWithConverter() {
        Single<Integer> single = Multi.createFrom().item((Integer) null).convert()
                .with(MultiRx3Converters.toSingle().onEmptyThrow(() -> new NoSuchElementException("not found")));
        assertThat(single).isNotNull();
        single
                .test()
                .assertFailure(NoSuchElementException.class)
                .assertNoValues();
    }

    @Test
    public void testCreatingASingleWithFailure() {
        Single<Optional<Integer>> single = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAMaybe() {
        Maybe<Integer> maybe = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAMaybeWithSubscriberAlreadyCancelled() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<MultiEmitter<? super Integer>> emitter = new AtomicReference<>();
        Maybe<Integer> maybe = Multi.createFrom().<Integer> emitter(emitter::set)
                .onCancellation().invoke(() -> cancelled.set(true))
                .convert().with(MultiRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        TestObserver<Integer> observer = maybe.test();
        observer.dispose();

        emitter.get().emit(1).complete();
        assertThat(observer.isDisposed()).isTrue();
        observer.assertEmpty();

        assertThat(cancelled).isTrue();
    }

    @Test
    public void testCreatingAMaybeFromNull() {
        Maybe<Integer> maybe = Multi.createFrom().item((Integer) null).convert().with(MultiRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAMaybeWithFailure() {
        Maybe<Integer> maybe = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAnObservable() {
        Observable<Integer> observable = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAnObservableFromNull() {
        Observable<Integer> observable = Multi.createFrom().item((Integer) null).convert()
                .with(MultiRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAnObservableWithFailure() {
        Observable<Integer> observable = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAFlowable() {
        Flowable<Integer> flowable = Multi.createFrom().item(1).convert().with(MultiRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAFlowableWithRequest() {
        AtomicBoolean called = new AtomicBoolean();
        TestSubscriber<Integer> subscriber = Multi.createFrom()
                .deferred(() -> Multi.createFrom().item(1).onItem().invoke((item) -> called.set(true)))
                .convert().with(MultiRx3Converters.toFlowable())
                .subscribeWith(TestSubscriber.create(0));

        assertThat(called).isFalse();
        subscriber.assertNoValues();
        subscriber.request(1);
        subscriber.assertComplete().assertValue(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingAFlowableFromNull() {
        Flowable<Integer> flowable = Multi.createFrom().item((Integer) null).convert()
                .with(MultiRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAFlowableWithFailure() {
        Flowable<Integer> flowable = Multi.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(MultiRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }
}
