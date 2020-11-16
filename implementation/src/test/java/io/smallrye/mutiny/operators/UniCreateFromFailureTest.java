package io.smallrye.mutiny.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

public class UniCreateFromFailureTest {

    @Test
    public void testWithASupplier() {
        Uni<Object> boom = Uni.createFrom().failure(() -> new IOException("boom"));
        try {
            boom.await().indefinitely();
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(IOException.class);
            return;
        }
        fail("Exception expected");
    }

    @Test
    public void testCreationWithCheckedException() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().failure(new Exception("boom")).subscribe().withSubscriber(subscriber);
        subscriber.assertFailedWith(Exception.class, "boom");

        try {
            Uni.createFrom().failure(new Exception("boom")).await().asOptional().indefinitely();
        } catch (Exception e) {
            assertThat(e).hasCauseInstanceOf(Exception.class)
                    .isInstanceOf(RuntimeException.class);
            return;
        }
        fail("Exception expected");
    }

    @Test
    public void testCreationWithRuntimeException() {
        UniAssertSubscriber<Object> subscriber = UniAssertSubscriber.create();
        Uni.createFrom().failure(new RuntimeException("boom")).subscribe().withSubscriber(subscriber);
        subscriber.assertFailedWith(RuntimeException.class, "boom");

        try {
            Uni.createFrom().failure(new RuntimeException("boom")).await().indefinitely();
        } catch (Exception e) {
            assertThat(e)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("boom");
            return;
        }
        Assertions.fail("Exception expected");
    }

    @Test
    public void testCreationWithNull() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().failure((Throwable) null));
    }

    @Test
    public void testCreationWithNullAsSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom().failure((Supplier<Throwable>) null));
    }

    @Test
    public void testWithASupplierReturningNull() {
        Uni<Object> boom = Uni.createFrom().failure(() -> null);
        try {
            boom.await().indefinitely();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NullPointerException.class);
            return;
        }
        Assertions.fail("Exception expected");
    }

    @Test
    public void testWithASupplierThrowingAnException() {
        Uni<Object> boom = Uni.createFrom().failure(() -> {
            throw new NoSuchElementException("boom");
        });
        try {
            boom.await().indefinitely();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(NoSuchElementException.class);
            return;
        }
        fail("Exception expected");
    }

}
