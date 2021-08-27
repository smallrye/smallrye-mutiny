package io.smallrye.mutiny.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.uni.UniRx3Converters;

@SuppressWarnings({ "CatchMayIgnoreException" })
public class UniConvertFromTest {

    @Test
    public void testCreatingFromACompletable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRx3Converters.fromCompletable(), Completable.complete());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        Uni<Void> uni = Uni.createFrom().converter(UniRx3Converters.fromCompletable(),
                Completable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromASingle() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromSingle(), Single.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromSingle(), Single.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMaybe() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromMaybe(), Maybe.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMaybeNeverEmitting() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromMaybe(), Maybe.<Integer> never()
                .doOnDispose(() -> cancelled.set(true)));
        assertThat(uni).isNotNull();
        uni.subscribe().with(i -> {
        }).cancel();
        assertThat(cancelled).isTrue();
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        Uni<Void> uni = Uni.createFrom().converter(UniRx3Converters.fromMaybe(), Maybe.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromMaybe(), Maybe.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlowable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromFlowable(), Flowable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromFlowable(), Flowable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRx3Converters.fromFlowable(), Flowable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromFlowable(), Flowable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAnObserver() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromObservable(), Observable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedObservable() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromObservable(), Observable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyObservable() {
        Uni<Void> uni = Uni.createFrom().converter(UniRx3Converters.fromObservable(), Observable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAnObservableWithFailure() {
        Uni<Integer> uni = Uni.createFrom().converter(UniRx3Converters.fromObservable(),
                Observable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }
}
