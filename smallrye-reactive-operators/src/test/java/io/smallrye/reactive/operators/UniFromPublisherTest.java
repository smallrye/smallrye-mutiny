package io.smallrye.reactive.operators;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import io.smallrye.reactive.Uni;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class UniFromPublisherTest {

    @Test
    public void testWithPublisher() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().publisher(Flowable.just(1)).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testWithPublisherBuilder() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni.createFrom().publisher(Flowable.just(1)).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
    }

    @Test
    public void testWithMultiValuedPublisher() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean cancelled = new AtomicBoolean();
        Uni.createFrom().publisher(Flowable.just(1, 2, 3).doOnCancel(() -> cancelled.set(true))).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(cancelled).isTrue();
    }


    @Test
    public void testWithException() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().publisher(Flowable.error(new IOException("boom"))).subscribe().withSubscriber(ts);
        ts.assertFailure(IOException.class, "boom");
    }

    @Test
    public void testWithEmptyStream() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().publisher(Flowable.empty()).subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(null);
    }

    @Test
    public void testThatValueIsNotEmittedBeforeSubscription() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().publisher(Flowable.generate(emitter -> {
            called.set(true);
            emitter.onNext(1);
            emitter.onComplete();
        }));


        assertThat(called).isFalse();
        uni.subscribe().withSubscriber(ts);
        ts.assertCompletedSuccessfully().assertItem(1);
        assertThat(called).isTrue();
    }

    @Test
    public void testThatSubscriberIsIncompleteIfThePublisherDoesNotEmit() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        AtomicBoolean called = new AtomicBoolean();
        Uni<Integer> uni = Uni.createFrom().<Integer>publisher(Flowable.never()).map(i -> {
            called.set(true);
            return i + 1;
        });

        assertThat(called).isFalse();

        uni.subscribe().withSubscriber(ts);
        assertThat(called).isFalse();
        ts.assertNotCompleted();
    }

    @Test
    public void testThatSubscriberCanCancelBeforeEmission() {
        UniAssertSubscriber<Integer> ts = UniAssertSubscriber.create();
        Uni<Integer> uni = Uni.createFrom().publisher(Flowable.<Integer>create(emitter -> {
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                emitter.onNext(1);
            }).start();
        }, BackpressureStrategy.DROP)).map(i -> i + 1);

        uni.subscribe().withSubscriber(ts);
        ts.cancel();

        ts.assertNotCompleted();
    }


}