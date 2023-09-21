///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.5.0-M3
package _02_groups;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.smallrye.mutiny.Uni;

public class _01_Uni_Event_Groups {

    public static void main(String[] args) {
        System.out.println("⚡️ Uni inspect events");

        var result = Uni.createFrom().item("Hello")
                .onSubscription().invoke(sub -> System.out.println("onSubscribe " + sub))
                .onCancellation().invoke(() -> System.out.println("onCancellation"))
                .onItem().invoke(item -> System.out.println("onItem " + item))
                .onFailure().invoke(failure -> System.out.println("onFailure " + failure))
                .onItemOrFailure().invoke((item, failure) -> System.out.println("onItemOrFailure " + item + ", " + failure))
                .onTermination().invoke((s, t, c) -> System.out.println("onTermination " + s + ", " + t + ", " + c))
                .await().atMost(Duration.of(5, ChronoUnit.SECONDS));

        System.out.println("📦 uni result = " + result);
        System.out.println("⚠️ This was a blocking operation");
    }
}
