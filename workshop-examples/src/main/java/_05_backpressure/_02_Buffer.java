///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0
package _05_backpressure;

import java.util.concurrent.Flow.Subscription;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class _02_Buffer {

    public static void main(String[] args) {
        System.out.println("⚡️ Back-pressure: buffer");

        Multi.createFrom().emitter(emitter -> emitTooFast(emitter), BackPressureStrategy.ERROR)
                .onOverflow().buffer(32)
                .subscribe().withSubscriber(new MultiSubscriber<Object>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(5);
                    }

                    @Override
                    public void onItem(Object s) {
                        System.out.print(s + " ");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("\n✋ " + throwable.getMessage());
                    }

                    @Override
                    public void onCompletion() {
                        System.out.println("\n✅");
                    }
                });
    }

    private static void emitTooFast(MultiEmitter<? super Object> emitter) {
        new Thread(() -> {
            while (true) {
                emitter.emit("📦");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
