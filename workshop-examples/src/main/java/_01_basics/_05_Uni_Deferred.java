///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.9.0
package _01_basics;

import java.util.concurrent.atomic.AtomicLong;

import io.smallrye.mutiny.Uni;

public class _05_Uni_Deferred {

    public static void main(String[] args) {
        System.out.println("️⚡️ Deferred Uni");

        AtomicLong ids = new AtomicLong();

        Uni<Long> deferredUni = Uni.createFrom().deferred(() -> Uni.createFrom().item(ids::incrementAndGet));

        for (var i = 0; i < 5; i++) {
            deferredUni.subscribe().with(System.out::println);
        }
    }
}
