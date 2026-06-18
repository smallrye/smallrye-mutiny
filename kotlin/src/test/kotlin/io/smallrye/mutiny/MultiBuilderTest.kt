package io.smallrye.mutiny

import io.smallrye.mutiny.helpers.test.AssertSubscriber
import io.smallrye.mutiny.subscription.BackPressureStrategy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class MultiBuilderTest {

    @Test
    fun `test build multi from items`() {
        val multi = multi<Int> {
            emit(1)
            emit(2)
            emit(3)
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems(1, 2, 3)
    }

    @Test
    fun `test build multi with explicit complete`() {
        val multi = multi<String> {
            emit("hello")
            complete()
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems("hello")
    }

    @Test
    fun `test build multi from failure`() {
        val multi = multi<String> {
            emit("before")
            fail(RuntimeException("Kaboom"))
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(RuntimeException::class.java, "Kaboom")
        assertThat(subscriber.items).containsExactly("before")
    }

    @Test
    fun `test build multi from exception in supplier`() {
        val multi = multi<String> {
            throw IllegalStateException("boom")
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(IllegalStateException::class.java, "boom")
    }

    @Test
    fun `test build empty multi`() {
        val multi = multi<String> { }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertHasNotReceivedAnyItem()
    }

    @Test
    fun `verify that supplier is called lazily`() {
        val supplierWasCalled = AtomicBoolean(false)
        val multi = multi<Int> {
            supplierWasCalled.set(true)
            emit(1)
        }
        assertThat(supplierWasCalled.get()).isFalse()
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion()
        assertThat(supplierWasCalled.get()).isTrue()
    }

    @Test
    fun `test isCancelled is accessible`() {
        val multi = multi<Int> {
            var i = 0
            while (!isCancelled && i < 100) {
                emit(i++)
            }
        }
        val subscriber = AssertSubscriber.create<Int>(5)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.request(100)
        subscriber.awaitCompletion()
    }

    @Test
    fun `test build multi with DROP back-pressure strategy`() {
        val multi = multi<Int>(backPressure = BackPressureStrategy.DROP) {
            emit(1)
            emit(2)
            emit(3)
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion()
        assertThat(subscriber.items).isNotEmpty
    }
}
