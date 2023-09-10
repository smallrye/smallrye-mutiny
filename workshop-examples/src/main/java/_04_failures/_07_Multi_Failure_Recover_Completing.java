///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _04_failures;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class _07_Multi_Failure_Recover_Completing {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi failure recover by completing");

        Multi.createFrom().emitter(emitter -> generate(emitter))
                .onFailure().recoverWithCompletion()
                .subscribe().with(System.out::println, Throwable::printStackTrace, () -> System.out.println("✅"));
    }

    private static void generate(MultiEmitter<? super Object> emitter) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (true) {
            if (random.nextDouble(0.0d, 1.0d) < 0.05d) {
                emitter.fail(new IOException("Boom"));
                return;
            } else {
                emitter.emit(random.nextInt(0, 100));
            }
        }
    }
}
