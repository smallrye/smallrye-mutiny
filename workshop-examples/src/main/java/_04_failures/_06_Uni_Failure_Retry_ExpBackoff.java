///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M5
package _04_failures;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class _06_Uni_Failure_Retry_ExpBackoff {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni failure retry");

        long start = System.currentTimeMillis();

        Uni.createFrom().emitter(emitter -> generate(emitter))
                .onFailure().invoke(() -> System.out.println("Failed after " + (System.currentTimeMillis() - start) + "ms"))
                .onFailure().retry().withBackOff(Duration.ofMillis(100), Duration.ofMillis(1000)).expireIn(10_000)
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
