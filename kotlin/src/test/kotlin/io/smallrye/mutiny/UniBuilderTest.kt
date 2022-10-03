package io.smallrye.mutiny

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class UniBuilderTest {

    @Test
    fun `test build uni from item`() {
        // Given
        val item = "wrap me plenty"

        // When
        val uni = uni { item }
        val subscriber = UniAssertSubscriber.create<String>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isSameAs(item)
    }

    @Test
    fun `test build uni from null`() {
        // Given & When
        val uni = uni { null }
        val subscriber = UniAssertSubscriber.create<Any?>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isNull()
    }

    @Test
    fun `test build uni from failure`() {
        // Given & When
        val uni = uni { throw Exception("Kaboom") }
        val subscriber = UniAssertSubscriber.create<Any?>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitFailure().failure).hasMessage("Kaboom")
    }

    @Test
    fun `verify that supplier is called lazily`() {
        // Given
        val supplierWasCalled = AtomicBoolean(false)

        // When uni is created
        val uni = uni { supplierWasCalled.set(true) }
        // Then the supplier is not called immediately
        assertThat(supplierWasCalled.get()).isFalse

        // When subscription happens
        val subscriber = UniAssertSubscriber.create<Any>()
        uni.subscribe().withSubscriber(subscriber)

        // Then supplier was called
        assertThat(supplierWasCalled.get()).isTrue
    }
}
