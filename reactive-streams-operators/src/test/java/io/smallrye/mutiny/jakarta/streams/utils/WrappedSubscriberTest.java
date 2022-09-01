package io.smallrye.mutiny.jakarta.streams.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class WrappedSubscriberTest {

    @Test
    public void checkThatOnSubscribeCanOnlyBeCallOnce() {
        AtomicReference<Subscription> subscriptionReference = new AtomicReference<>();
        WrappedSubscriber<Integer> subscriber = new WrappedSubscriber<>(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionReference.set(s);
            }

            @Override
            public void onNext(Integer integer) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        AtomicBoolean cancellationReference1 = new AtomicBoolean();
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {
                cancellationReference1.set(true);
            }
        });

        assertThat(cancellationReference1.get()).isFalse();
        assertThat(subscriptionReference.get()).isNotNull();

        AtomicBoolean cancellationReference2 = new AtomicBoolean();
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {
                cancellationReference2.set(true);
            }
        });

        assertThat(cancellationReference1.get()).isFalse();
        assertThat(cancellationReference2.get()).isTrue();
        assertThat(subscriptionReference.get()).isNotNull();

    }

}
