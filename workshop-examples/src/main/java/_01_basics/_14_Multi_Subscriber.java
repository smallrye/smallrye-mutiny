///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M3
package _01_basics;

import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Multi;

public class _14_Multi_Subscriber {

    public static void main(String[] args) {
        System.out.println("⚡️ Hello world with subscriber");

        Multi.createFrom().items(1, 2, 3).subscribe().withSubscriber(new Subscriber<Integer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("onSubscribe()");
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("onNext: " + integer);
                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError: " + t.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete()");
            }
        });
    }
}
