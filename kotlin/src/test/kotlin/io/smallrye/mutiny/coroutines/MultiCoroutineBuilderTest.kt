package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.AssertSubscriber
import io.smallrye.mutiny.subscription.BackPressureStrategy
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat

@ExperimentalCoroutinesApi
class MultiCoroutineBuilderTest {

    @Test
    fun `test build multi from suspend items`() {
        val multi = multi<Int> {
            delay(1.milliseconds)
            emit(1)
            delay(1.milliseconds)
            emit(2)
            delay(1.milliseconds)
            emit(3)
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems(1, 2, 3)
    }

    @Test
    fun `test build multi from suspend with failure`() {
        val multi = multi<String> {
            delay(1.milliseconds)
            throw RuntimeException("Kaboom")
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(RuntimeException::class.java, "Kaboom")
    }

    @Test
    fun `test build multi from suspend with explicit fail`() {
        val multi = multi<String> {
            delay(1.milliseconds)
            emit("before")
            fail(IllegalStateException("boom"))
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(IllegalStateException::class.java, "boom")
        assertThat(subscriber.items).containsExactly("before")
    }

    @Test
    fun `test build empty multi from suspend`() {
        val multi = multi<String> {
            delay(1.milliseconds)
        }
        val subscriber = AssertSubscriber.create<String>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertHasNotReceivedAnyItem()
    }

    @Test
    fun `test build multi with custom scope`() {
        val multi = multi<Int>(context = embeddedScope()) {
            delay(1.milliseconds)
            emit(42)
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion().assertItems(42)
    }

    @Test
    fun `test build multi with DROP back-pressure strategy`() {
        val multi = multi<Int>(backPressure = BackPressureStrategy.DROP) {
            delay(1.milliseconds)
            emit(1)
            emit(2)
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitCompletion()
        assertThat(subscriber.items).isNotEmpty
    }

    @Test
    fun `test exception after emitting items delivers items then failure`() {
        val multi = multi<Int> {
            delay(1.milliseconds)
            emit(1)
            emit(2)
            throw RuntimeException("late boom")
        }
        val subscriber = AssertSubscriber.create<Int>(10)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitFailure().assertFailedWith(RuntimeException::class.java, "late boom")
        assertThat(subscriber.items).containsExactly(1, 2)
    }

    @Test
    fun `test cancellation cancels the coroutine`() {
        val multi = multi<Int> {
            var i = 0
            while (!isCancelled) {
                emit(i++)
                delay(10.milliseconds)
            }
        }
        val subscriber = AssertSubscriber.create<Int>(5)
        multi.subscribe().withSubscriber(subscriber)
        subscriber.awaitItems(5)
        subscriber.cancel()
        val countAfterCancel = subscriber.items.size
        Thread.sleep(100)
        assertThat(subscriber.items.size).isEqualTo(countAfterCancel)
    }
}
