///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.4.0
package _04_failures;

import io.smallrye.mutiny.Multi;

public class _11_Multi_Failure_Cancelling_Recovery {

    public static void main(String[] args) {
        System.out.println("⚡️ Multi failure that cancels after recovery");

        Multi.createFrom().range(0, 10)
                .onItem().invoke(n -> {
                    if (n == 6) {
                        throw new RuntimeException("Bada Boom");
                    } else {
                        System.out.println(n + " 👍");
                    }
                })
                .onFailure().recoverWithItem(6)
                .subscribe().with(System.out::println, Throwable::printStackTrace, () -> System.out.println("✅"));
    }
}
