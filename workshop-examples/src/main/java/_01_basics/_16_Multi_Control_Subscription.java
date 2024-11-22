///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC5
package _01_basics;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class _16_Multi_Control_Subscription {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi and subscription");

        Multi.createFrom()
                .ticks().every(Duration.of(1, ChronoUnit.SECONDS))
                .subscribe().withSubscriber(new MultiSubscriber<Long>() {

                    private Subscription subscription;
                    private int counter = 0;

                    @Override
                    public void onItem(Long tick) {
                        System.out.println("Tick: " + tick);
                        if (counter++ == 10) {
                            subscription.cancel();
                        } else {
                            subscription.request(1);
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onCompletion() {
                        System.out.println("Done!");
                    }

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }
                });

    }
}
