package io.smallrye.reactive.adapt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.reactivex.*;
import io.smallrye.reactive.Uni;
import io.smallrye.reactive.adapt.converters.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class UniAdaptFromTest {

    @Test
    public void testCreatingFromACompletable() {
        Uni<Void> uni = Uni.createWith(FromCompletable.INSTANCE, Completable.complete());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromACompletableFromVoid() {
        Uni<Void> uni = Uni.createWith(FromCompletable.INSTANCE, Completable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromASingle() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromSingle(), Single.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromASingleWithFailure() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromSingle(), Single.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMaybe() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromMaybe(), Maybe.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMaybe() {
        Uni<Void> uni = Uni.createWith(BuiltinConverters.fromMaybe(), Maybe.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMaybeWithFailure() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromMaybe(), Maybe.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlowable() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromFlowable(), Flowable.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlowable() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromFlowable(), Flowable.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlowable() {
        Uni<Void> uni = Uni.createWith(BuiltinConverters.fromFlowable(), Flowable.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFlowableWithFailure() {
        Uni<Integer> uni = Uni.createWith(BuiltinConverters.fromFlowable(), Flowable.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAMono() {
        Uni<Integer> uni = Uni.createWith(new FromMono<>(), Mono.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyMono() {
        Uni<Void> uni = Uni.createWith(new FromMono<>(), Mono.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAMonoWithFailure() {
        Uni<Integer> uni = Uni.createWith(new FromMono<>(), Mono.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromAFlux() {
        Uni<Integer> uni = Uni.createWith(new FromFlux<>(), Flux.just(1));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAMultiValuedFlux() {
        Uni<Integer> uni = Uni.createWith(new FromFlux<>(), Flux.just(1, 2, 3));
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isEqualTo(1);
    }

    @Test
    public void testCreatingFromAnEmptyFlux() {
        Uni<Void> uni = Uni.createWith(new FromFlux<>(), Flux.empty());
        assertThat(uni).isNotNull();
        assertThat(uni.await().indefinitely()).isNull();
    }

    @Test
    public void testCreatingFromAFluxWithFailure() {
        Uni<Integer> uni = Uni.createWith(new FromFlux<>(), Flux.error(new IOException("boom")));
        assertThat(uni).isNotNull();
        try {
            uni.await().indefinitely();
            fail("Exception expected");
        } catch (RuntimeException e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreatingFromCompletionStages() {
        CompletableFuture<Integer> valued = CompletableFuture.completedFuture(1);
        CompletableFuture<Void> empty = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> boom = new CompletableFuture<>();
        boom.completeExceptionally(new Exception("boom"));

        Uni<Integer> u1 = Uni.createFrom().completionStage(valued);
        Uni<Void> u2 = Uni.createFrom().completionStage(empty);
        Uni<Void> u3 = Uni.createWith(BuiltinConverters.fromCompletionStage(), boom);

        assertThat(u1.await().asOptional().indefinitely()).contains(1);
        assertThat(u2.await().indefinitely()).isEqualTo(null);
        try {
            u3.await();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class).hasCauseInstanceOf(Exception.class);
        }
    }

}
