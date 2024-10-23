///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0-RC2
package _01_basics;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.mutiny.Uni;

public class _04_Uni_From_Supplier_And_State {

    public static void main(String[] args) {
        System.out.println("️⚡️ Uni from supplier with state");

        Uni<Integer> uniFromSupplierAndState = Uni.createFrom().item(AtomicInteger::new, i -> i.addAndGet(10));

        for (var i = 0; i < 5; i++) {
            uniFromSupplierAndState.subscribe().with(System.out::println);
        }
    }
}
