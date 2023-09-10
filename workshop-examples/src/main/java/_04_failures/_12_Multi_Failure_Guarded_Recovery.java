///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _04_failures;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _12_Multi_Failure_Guarded_Recovery {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi failure that does not cancel after recovery");

        Multi.createFrom().range(0, 10)
                .onItem().transformToUniAndConcatenate(i -> safeGuardedOperation(i))
                .onFailure().recoverWithItem(6)
                .subscribe().with(System.out::println, Throwable::printStackTrace, () -> System.out.println("✅"));
    }

    private static Uni<Integer> safeGuardedOperation(Integer i) {
        return Uni
                .createFrom().item(i)
                .onItem().invoke(n -> {
                    if (n == 6) {
                        throw new RuntimeException("Bada Boom");
                    } else {
                        System.out.println(n + " 👍");
                    }
                })
                .onFailure().recoverWithItem(i);
    }
}
