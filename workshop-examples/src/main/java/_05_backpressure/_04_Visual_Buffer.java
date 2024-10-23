///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC3
package _05_backpressure;

import java.util.concurrent.Flow.Subscription;
import java.util.function.Consumer;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class _04_Visual_Buffer {

    public static void main(String[] args) {
        System.out.println("⚡️ Back-pressure: buffering visualised");

        Multi.createFrom().emitter(emitter -> emitTooFast(emitter), BackPressureStrategy.ERROR)
                .onItem().invoke((Consumer<Object>) System.out::println)
                .onOverflow().buffer(64)
                .subscribe().withSubscriber(new MultiSubscriber<Object>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(5);
                        periodicallyRequest(s);
                    }

                    private void periodicallyRequest(Subscription s) {
                        new Thread(() -> {
                            while (true) {
                                try {
                                    Thread.sleep(5_000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("           🤷 request");
                                s.request(2);
                            }
                        }).start();
                    }

                    @Override
                    public void onItem(Object s) {
                        System.out.println("           ➡️ " + s);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        System.out.println("✋ " + throwable.getMessage());
                    }

                    @Override
                    public void onCompletion() {
                        System.out.println("✅");
                    }
                });
    }

    private static void emitTooFast(MultiEmitter<? super Object> emitter) {
        new Thread(() -> {
            long n = 0;
            while (true) {
                emitter.emit("📦 " + ++n);
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
