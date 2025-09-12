///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.smallrye.reactive:mutiny:2.9.4
package _03_composition_transformation;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.AbstractMultiOperator;
import io.smallrye.mutiny.operators.multi.MultiOperatorProcessor;
import io.smallrye.mutiny.subscription.MultiSubscriber;

public class _22_Multi_Chunks_To_Sentence_Stream_Custom_Operator {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("⚡️ Chunks of text to sentence stream (with a custom operator)");

        List<String> chunks = List.of(
                "Hel",
                "lo ",
                "world\n",
                "Foo",
                " B",
                "ar ",
                "Baz\n");

        Multi.createFrom().iterable(chunks)
                .plug(TokenToSentence::new)
                .onItem().transformToUniAndConcatenate(line -> sendText(line))
                .subscribe().with(
                        line -> System.out.println(">>> " + line),
                        Throwable::printStackTrace,
                        pool::shutdownNow);

    }

    static class TokenToSentence extends AbstractMultiOperator<String, String> {

        public TokenToSentence(Multi<? extends String> upstream) {
            super(upstream);
        }

        @Override
        public void subscribe(MultiSubscriber<? super String> downstream) {
            upstream.subscribe().withSubscriber(new TokenToSentenceProcessor(downstream));
        }

        static private class TokenToSentenceProcessor extends MultiOperatorProcessor<String, String> {

            private final StringBuilder builder = new StringBuilder();

            public TokenToSentenceProcessor(MultiSubscriber<? super String> downstream) {
                super(downstream);
            }

            @Override
            public void onItem(String chunk) {
                builder.append(chunk);
                String current = builder.toString();
                if (current.endsWith("\n")) {
                    builder.setLength(0);
                    super.onItem(current.substring(0, current.length() - 1));
                } else {
                    upstream.request(1L);
                }
            }
        }
    }

    static final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    static Uni<String> sendText(String text) {
        return Uni.createFrom().item(text)
                .onItem().delayIt().onExecutor(pool).by(Duration.ofMillis(300))
                .onItem().invoke(txt -> System.out.println("[sendText] " + txt));
    }
}
