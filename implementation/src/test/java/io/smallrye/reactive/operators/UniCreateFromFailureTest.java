package io.smallrye.reactive.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.Test;

import io.smallrye.reactive.Uni;

public class UniCreateFromFailureTest {

    @Test
    public void testWithASupplier() {
        Uni<Object> boom = Uni.createFrom().deferredFailure(() -> new IOException("boom"));
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }

    @Test
    public void testCreationWithCheckedException() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().failure(new Exception("boom")).subscribe().withSubscriber(ts);
        ts.assertFailure(Exception.class, "boom");

        try {
            Uni.createFrom().failure(new Exception("boom")).await().asOptional().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class)
                    .isInstanceOf(RuntimeException.class);
        }

    }

    @Test
    public void testCreationWithRuntimeException() {
        UniAssertSubscriber<Object> ts = UniAssertSubscriber.create();
        Uni.createFrom().failure(new RuntimeException("boom")).subscribe().withSubscriber(ts);
        ts.assertFailure(RuntimeException.class, "boom");

        try {
            Uni.createFrom().failure(new RuntimeException("boom")).await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("boom");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreationWithNull() {
        Uni.createFrom().failure((Exception) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreationWithNullAsSupplier() {
        Uni.createFrom().deferredFailure((Supplier<Throwable>) null);
    }

    @Test
    public void testWithASupplierReturningNull() {
        Uni<Object> boom = Uni.createFrom().deferredFailure(() -> null);
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testWithASupplierThrowingAnException() {
        Uni<Object> boom = Uni.createFrom().deferredFailure(() -> {
            throw new NoSuchElementException("boom");
        });
        try {
            boom.await().indefinitely();
            fail("Exception expected");
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
        }
    }

}
