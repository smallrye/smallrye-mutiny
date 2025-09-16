/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:3.0.0
package _03_composition_transformation;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.Gatherer.Extraction;

public class _22_Multi_Chunks_To_Sentence_Gather {

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

        Multi.createFrom().iterable(chunks)
                .onItem().gather()
                .into(StringBuilder::new)
                .accumulate(StringBuilder::append)
                .extract((sb, completed) -> {
                    String str = sb.toString();
                    if (str.contains("\n")) {
                        String[] lines = str.split("\n", 2);
                        return Optional.of(Extraction.of(new StringBuilder(lines[1]), lines[0]));
                    }
                    return Optional.empty();
                })
                .finalize(sb -> Optional.of(sb.toString()))
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
