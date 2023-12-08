///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.2
package _04_failures;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;

public class _08_Multi_Failure_Recover_With_Item {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi failure recover with item");

        Multi.createFrom().emitter(emitter -> generate(emitter))
                .onFailure().recoverWithItem(() -> 666)
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
