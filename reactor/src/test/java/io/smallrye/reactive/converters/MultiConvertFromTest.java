package io.smallrye.reactive.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;

import org.junit.Test;

import io.smallrye.reactive.Multi;
import io.smallrye.reactive.converters.multi.ReactorConverters;
import reactor.core.publisher.Mono;

public class MultiConvertFromTest {

    @Test
    public void testCreatingFromAMono() {
        Multi<Integer> multi = Multi.createFrom().converter(ReactorConverters.fromMono(), Mono.just(1));
        assertThat(multi).isNotNull();
        multi.onItem().consume(i -> assertThat(i).isEqualTo(1));
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        Multi<Void> multi = Multi.createFrom().converter(ReactorConverters.fromMono(), Mono.empty());
        assertThat(multi).isNotNull();
        multi.onItem().consume(i -> assertThat(i).isEqualTo(null));
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        Multi<Integer> multi = Multi.createFrom().converter(ReactorConverters.fromMono(), Mono.error(new IOException("boom")));
        assertThat(multi).isNotNull();
        try {
            //TODO Not sure this is right
            multi.collect().first().await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }
}
