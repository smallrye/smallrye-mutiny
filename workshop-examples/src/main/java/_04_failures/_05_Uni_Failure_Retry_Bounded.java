///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.9.1
package _04_failures;

import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class _05_Uni_Failure_Retry_Bounded {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni failure retry");

        Uni.createFrom().emitter(emitter -> generate(emitter))
                .onFailure().invoke(() -> System.out.println("Failed"))
                .onFailure().retry().atMost(10)
                .subscribe().with(System.out::println, Throwable::printStackTrace);
    }

    private static void generate(UniEmitter<? super Object> emitter) {
        if (ThreadLocalRandom.current().nextDouble(0.0d, 1.0d) < 0.05d) {
            emitter.complete("Ok");
        } else {
            emitter.fail(new RuntimeException("Boom"));
        }
    }
}
