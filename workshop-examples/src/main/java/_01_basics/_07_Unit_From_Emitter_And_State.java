///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:3.0.0
package _01_basics;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Uni;

public class _07_Unit_From_Emitter_And_State {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni from emitter and state");

        Uni<Integer> uniFromEmitterAndState = Uni.createFrom()
                .emitter(AtomicInteger::new, (i, e) -> e.complete(i.addAndGet(10)));

        for (var i = 0; i < 5; i++) {
            uniFromEmitterAndState.subscribe().with(System.out::println);
        }
    }
}
