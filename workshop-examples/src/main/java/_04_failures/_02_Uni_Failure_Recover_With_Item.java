///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.7.0
package _04_failures;

import java.io.IOException;

import io.smallrye.mutiny.Uni;

public class _02_Uni_Failure_Recover_With_Item {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni failure recover with item");

        Uni.createFrom().failure(new IOException("Boom"))
                .onFailure(IOException.class).recoverWithItem(Throwable::getMessage)
                .subscribe().with(System.out::println, Throwable::printStackTrace);
    }
}
