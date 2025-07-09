///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.9.3
package _03_composition_transformation;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class _22_Multi_Chunks_To_Sentence_Stream {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Chunks of text to sentence stream");

        List<String> chunks = List.of(
                "Hel",
                "lo ",
                "world\n",
                "Foo",
                " B",
                "ar ",
                "Baz\n");

        StringBuilder builder = new StringBuilder();
        Multi.createFrom().iterable(chunks)
                .onItem().transformToUniAndConcatenate(chunk -> {
                    builder.append(chunk);
                    String current = builder.toString();
                    if (current.endsWith("\n")) {
                        builder.setLength(0);
                        return Uni.createFrom().item(current.substring(0, current.length() - 1));
                    } else {
                        return Uni.createFrom().nullItem();
                    }
                })
                .onItem().transformToUniAndConcatenate(line -> sendText(line))
                .subscribe().with(
                        line -> System.out.println(">>> " + line),
                        Throwable::printStackTrace,
                        pool::shutdownNow);

    }

    static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    static Uni<String> sendText(String text) {
        return Uni.createFrom().item(text)
                .onItem().delayIt().onExecutor(pool).by(Duration.ofMillis(300))
                .onItem().invoke(txt -> System.out.println("[sendText] " + txt));
    }
}
