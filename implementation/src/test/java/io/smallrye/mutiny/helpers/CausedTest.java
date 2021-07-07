package io.smallrye.mutiny.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

public class CausedTest {

    private static final String GENERIC = "Generic";
    private static final String SPECIFIC = "Specific";

    @Test
    public void testWithWrappedCheckedException() {
        assertEquals(SPECIFIC, Uni.createFrom()
                .item(Unchecked.supplier(this::stringSupplierThrowingIOException))
                .onFailure(Caused.by(IOException.class))
                .recoverWithItem(SPECIFIC)
                .onFailure()
                .recoverWithItem(GENERIC)
                .await()
                .indefinitely());
    }

    @Test
    public void testWithWrappedCheckedExceptionSubclass() {
        assertEquals(SPECIFIC, Uni.createFrom()
                .item(Unchecked.supplier(this::stringSupplierThrowingFileNotFoundException))
                .onFailure(Caused.by(IOException.class))
                .recoverWithItem(SPECIFIC)
                .onFailure()
                .recoverWithItem(GENERIC)
                .await()
                .indefinitely());
    }

    @Test
    public void testWithUncheckedException() {
        assertEquals(SPECIFIC, Uni.createFrom()
                .item(Unchecked.supplier(this::numberSupplierThrowingIllegalArgumentException))
                .onFailure(Caused.by(IllegalArgumentException.class))
                .recoverWithItem(SPECIFIC)
                .onFailure()
                .recoverWithItem(GENERIC)
                .await()
                .indefinitely());
    }

    @Test
    public void testWithUncheckedExceptionSubclass() {
        assertEquals(SPECIFIC, Uni.createFrom()
                .item(Unchecked.supplier(this::numberSupplierThrowingNumberFormatException))
                .onFailure(Caused.by(IllegalArgumentException.class))
                .recoverWithItem(SPECIFIC)
                .onFailure()
                .recoverWithItem(GENERIC)
                .await()
                .indefinitely());
    }

    @Test
    public void testWithNullParameter() {
        assertThrows(IllegalArgumentException.class, () -> Uni.createFrom()
                .item(Unchecked.supplier(this::numberSupplierThrowingNumberFormatException))
                .onFailure(Caused.by(null))
                .recoverWithItem(SPECIFIC)
                .onFailure()
                .recoverWithItem(GENERIC)
                .await()
                .indefinitely());
    }

    private String stringSupplierThrowingIOException() throws IOException {
        throw new IOException();
    }

    private String stringSupplierThrowingFileNotFoundException() throws FileNotFoundException {
        throw new FileNotFoundException();
    }

    private String numberSupplierThrowingIllegalArgumentException() {
        throw new IllegalArgumentException();
    }

    private String numberSupplierThrowingNumberFormatException() {
        throw new NumberFormatException();
    }

}
