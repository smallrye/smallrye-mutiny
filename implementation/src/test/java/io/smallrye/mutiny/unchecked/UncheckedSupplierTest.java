package io.smallrye.mutiny.unchecked;

import static io.smallrye.mutiny.unchecked.Unchecked.supplier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

public class UncheckedSupplierTest {

    @Test
    public void testUncheckedSupplier() {
        Supplier<Integer> supplier = supplier(() -> 1);
        Supplier<Integer> supplierFailingWithIo = supplier(() -> {
            throw new IOException("boom");
        });
        Supplier<Integer> supplierFailingWithArithmetic = supplier(() -> {
            throw new ArithmeticException("boom");
        });

        assertThat(supplier.get()).isEqualTo(1);
        assertThatThrownBy(supplierFailingWithIo::get)
                .hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
        assertThatThrownBy(supplierFailingWithArithmetic::get)
                .isInstanceOf(ArithmeticException.class).hasMessageContaining("boom");
    }

    @Test
    public void testCreatingAUni() {
        assertThat(Uni.createFrom().item(supplier(() -> 1)).await().indefinitely()).isEqualTo(1);
        assertThatThrownBy(() -> Uni.createFrom().item(supplier(() -> {
            throw new IOException("boom");
        })).await().indefinitely()).hasCauseInstanceOf(IOException.class).hasMessageContaining("boom");
    }

}
