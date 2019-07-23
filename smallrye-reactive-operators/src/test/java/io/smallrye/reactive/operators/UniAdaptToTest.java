package io.smallrye.reactive.operators;

import io.reactivex.*;
import io.reactivex.subscribers.TestSubscriber;
import io.smallrye.reactive.Uni;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class UniAdaptToTest {

    @Test
    public void testCreatingACompletable() {
        Completable completable = Uni.createFrom().result(1).adapt().to(Completable.class);
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testThatSubscriptionOnCompletableProducesTheValue() {
        AtomicBoolean called = new AtomicBoolean();
        Completable completable =  Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().result(2);
        }).adapt().to(Completable.class);

        assertThat(completable).isNotNull();
        assertThat(called).isFalse();
        completable.test().assertComplete();
        assertThat(called).isTrue();
    }

    @Test
    public void testCreatingACompletableFromVoid() {
        Completable completable = Uni.createFrom().nullValue().adapt().to(Completable.class);
        assertThat(completable).isNotNull();
        completable.test().assertComplete();
    }

    @Test
    public void testCreatingACompletableWithFailure() {
        Completable completable = Uni.createFrom().failure(new IOException("boom")).adapt().to(Completable.class);
        assertThat(completable).isNotNull();
        completable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingASingle() {
        Single<Integer> single = Uni.createFrom().result(1).adapt().to(Single.class);
        assertThat(single).isNotNull();
        single.test()
                .assertValue(1)
                .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingASingleFromNull() {
        Single<Integer> single = Uni.createFrom().nullValue().adapt().to(Single.class);
        assertThat(single).isNotNull();
        single
                .test()
                .assertError(NoSuchElementException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingASingleWithFailure() {
        Single<Integer> single = Uni.createFrom().failure(new IOException("boom")).adapt().to(Single.class);
        assertThat(single).isNotNull();
        single.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMaybe() {
        Maybe<Integer> maybe = Uni.createFrom().result(1).adapt().to(Maybe.class);
        assertThat(maybe).isNotNull();
        maybe.test()
                .assertValue(1)
                .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMaybeFromNull() {
        Maybe<Integer> maybe = Uni.createFrom().nullValue().adapt().to(Maybe.class);
        assertThat(maybe).isNotNull();
        maybe
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMaybeWithFailure() {
        Maybe<Integer> maybe = Uni.createFrom().failure(new IOException("boom")).adapt().to(Maybe.class);
        assertThat(maybe).isNotNull();
        maybe.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAnObservable() {
        Observable<Integer> observable = Uni.createFrom().result(1).adapt().to(Observable.class);
        assertThat(observable).isNotNull();
        observable.test()
                .assertValue(1)
                .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAnObservableFromNull() {
        Observable<Integer> observable = Uni.createFrom().nullValue().adapt().to(Observable.class);
        assertThat(observable).isNotNull();
        observable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAnObservableWithFailure() {
        Observable<Integer> observable = Uni.createFrom().failure(new IOException("boom")).adapt().to(Observable.class);
        assertThat(observable).isNotNull();
        observable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlowable() {
        Flowable<Integer> flowable = Uni.createFrom().result(1).adapt().to(Flowable.class);
        assertThat(flowable).isNotNull();
        flowable.test()
                .assertValue(1)
                .assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlowableWithRequest() {
        AtomicBoolean called = new AtomicBoolean();
        Flowable<Integer> flowable =  Uni.createFrom().deferred(() -> {
            called.set(true);
            return Uni.createFrom().result(1);
        }).adapt().to(Flowable.class);
        assertThat(flowable).isNotNull();
        TestSubscriber<Integer> test = flowable.test(0);
        assertThat(called).isFalse();
        test.assertNoValues().assertSubscribed();
        test.request(2);
        test.assertValue(1).assertComplete();
        assertThat(called).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlowableFromNull() {
        Flowable<Integer> flowable = Uni.createFrom().nullValue().adapt().to(Flowable.class);
        assertThat(flowable).isNotNull();
        flowable
                .test()
                .assertComplete()
                .assertNoValues();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlowableWithFailure() {
        Flowable<Integer> flowable = Uni.createFrom().failure(new IOException("boom")).adapt().to(Flowable.class);
        assertThat(flowable).isNotNull();
        flowable.test().assertError(e -> {
            assertThat(e).hasMessage("boom").isInstanceOf(IOException.class);
            return true;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFlux() {
        Flux<Integer> flux = Uni.createFrom().result(1).adapt().to(Flux.class);
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFluxFromNull() {
        Flux<Integer> flux = Uni.createFrom().nullValue().adapt().to(Flux.class);
        assertThat(flux).isNotNull();
        assertThat(flux.blockFirst()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAFluxWithFailure() {
        Flux<Integer> flux = Uni.createFrom().failure(new IOException("boom")).adapt().to(Flux.class);
        assertThat(flux).isNotNull();
        try {
            flux.blockFirst();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMono() {
        Mono<Integer> mono = Uni.createFrom().result(1).adapt().to(Mono.class);
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isEqualTo(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMonoFromNull() {
        Mono<Integer> mono = Uni.createFrom().nullValue().adapt().to(Mono.class);
        assertThat(mono).isNotNull();
        assertThat(mono.block()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingAMonoWithFailure() {
        Mono<Integer> mono = Uni.createFrom().failure(new IOException("boom")).adapt().to(Mono.class);
        assertThat(mono).isNotNull();
        try {
            mono.block();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingCompletionStages() {
        Uni<Integer> valued = Uni.createFrom().result(1);
        Uni<Void> empty = Uni.createFrom().nullValue();
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletionStage<Integer> stage1 = valued.adapt().to(CompletionStage.class);
        CompletionStage<Void> stage2 = empty.adapt().to(CompletionStage.class);
        CompletionStage<Void> stage3 = failure.adapt().to(CompletionStage.class);

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreatingCompletableFutures() {
        Uni<Integer> valued = Uni.createFrom().result(1);
        Uni<Void> empty = Uni.createFrom().nullValue();
        Uni<Void> failure = Uni.createFrom().failure(new Exception("boom"));

        CompletableFuture<Integer> stage1 = valued.adapt().to(CompletableFuture.class);
        CompletableFuture<Void> stage2 = empty.adapt().to(CompletableFuture.class);
        CompletableFuture<Void> stage3 = failure.adapt().to(CompletableFuture.class);

        assertThat(stage1).isCompletedWithValue(1);
        assertThat(stage2).isCompletedWithValue(null);
        assertThat(stage3).isCompletedExceptionally();
    }
}