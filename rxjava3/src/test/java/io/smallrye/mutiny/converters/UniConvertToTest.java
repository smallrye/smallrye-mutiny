package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.ToSingle;
import io.smallrye.mutiny.converters.uni.ToSingleWithDefault;
import io.smallrye.mutiny.converters.uni.UniRx3Converters;
import io.smallrye.mutiny.subscription.UniEmitter;

public class UniConvertToTest {

    @Test
    public void testCreatingACompletable() {
        Completable completable = Uni.createFrom().item(1).convert().with(UniRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testThatSubscriptionOnCompletableProducesTheValue() {
        AtomicBoolean called = new AtomicBoolean();
        Completable completable = Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().item(2);
        }).convert().with(UniRx3Converters.toCompletable());

        assertThat(completable).isNotNull();
        assertThat(called).isFalse();
        completable.test().assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingACompletableFromVoid() {
        Completable completable = Uni.createFrom().item((Object) null).convert().with(UniRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testCreatingACompletableWithFailure() {
        Completable completable = Uni.createFrom().failure(new IOException("boom")).convert()
                .with(UniRx3Converters.toCompletable());
        assertThat(completable).isNotNull();
        completable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingASingle() {
        Single<Optional<Integer>> single = Uni.createFrom().item(1).convert().with(UniRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleByConverter() {
        Single<Optional<Integer>> single = Uni.createFrom().item(1).convert().with(UniRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test()
                .assertValue(Optional.of(1))
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNull() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultWithValue() {
        Single<Integer> single = Uni.createFrom().item(5).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(5)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNullAlternative() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(ToSingle.withDefault(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithDefaultFromNullByConverter() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert().with(new ToSingleWithDefault<>(22));
        assertThat(single).isNotNull();
        single.test()
                .assertValue(22)
                .assertComplete();
    }

    @Test
    public void testFailureToCreateSingleFromNull() {
        Single<Integer> single = Uni.createFrom().item((Integer) null).convert()
                .with(UniRx3Converters.toSingle().failOnNull());
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
            return true;
        });
    }

    @Test
    public void testCreatingASingleFromNull() {
        Single<Optional<Integer>> single = Uni.createFrom().item((Integer) null).convert()
                .with(UniRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single
                .test()
                .assertValue(Optional.empty())
                .assertComplete();
    }

    @Test
    public void testCreatingASingleWithFailure() {
        Single<Optional<Integer>> single = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRx3Converters.toSingle());
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAMaybe() {
        Maybe<Integer> maybe = Uni.createFrom().item(1).convert().with(UniRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAMaybeWithSubscriberAlreadyCancelled() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicReference<UniEmitter<? super Integer>> emitter = new AtomicReference<>();
        Maybe<Integer> maybe = Uni.createFrom().<Integer> emitter(emitter::set)
                .onCancellation().invoke(() -> cancelled.set(true))
                .convert().with(UniRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        TestObserver<Integer> observer = maybe.test();
        observer.dispose();

        emitter.get().complete(1);
        assertThat(observer.isDisposed()).isTrue();
        observer.assertEmpty();

        assertThat(cancelled).isTrue();
    }

    @Test
    public void testCreatingAMaybeFromNull() {
        Maybe<Integer> maybe = Uni.createFrom().item((Integer) null).convert().with(UniRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAMaybeWithFailure() {
        Maybe<Integer> maybe = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRx3Converters.toMaybe());
        assertThat(maybe).isNotNull();
        maybe.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAnObservable() {
        Observable<Integer> observable = Uni.createFrom().item(1).convert().with(UniRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAnObservableFromNull() {
        Observable<Integer> observable = Uni.createFrom().item((Integer) null).convert()
                .with(UniRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAnObservableWithFailure() {
        Observable<Integer> observable = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRx3Converters.toObservable());
        assertThat(observable).isNotNull();
        observable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @Test
    public void testCreatingAFlowable() {
        Flowable<Integer> flowable = Uni.createFrom().item(1).convert().with(UniRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test()
                .assertValue(1)
                .assertComplete();
    }

    @Test
    public void testCreatingAFlowableWithRequest() {
        AtomicBoolean called = new AtomicBoolean();
        Flowable<Integer> flowable = Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().item(1);
        }).convert().with(UniRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        TestSubscriber<Integer> test = flowable.test(0);
        assertThat(called).isFalse();
        test.assertNoValues();
        test.request(2);
        test.assertValue(1).assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingAFlowableFromNull() {
        Flowable<Integer> flowable = Uni.createFrom().item((Integer) null).convert().with(UniRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @Test
    public void testCreatingAFlowableWithFailure() {
        Flowable<Integer> flowable = Uni.createFrom().<Integer> failure(new IOException("boom")).convert()
                .with(UniRx3Converters.toFlowable());
        assertThat(flowable).isNotNull();
        flowable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }
}
