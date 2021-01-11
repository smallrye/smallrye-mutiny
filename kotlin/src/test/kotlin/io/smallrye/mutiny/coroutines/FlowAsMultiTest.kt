package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.AssertSubscriber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test

class FlowAsMultiTest {

    @Test
    fun `test immediate item`() {
        runBlocking(Dispatchers.Default) {
            // Given
            val item = UUID.randomUUID()
            val flow = flowOf(item)

            // When
            val subscriber = AssertSubscriber.create<UUID>(1)
            val multi = flow.asMulti()
            multi.subscribe().withSubscriber(subscriber)
            val collectedItems = multi.collect().asList().await().indefinitely()

            // Then
            assertThat(collectedItems).containsExactly(item)
            subscriber.await().assertItems(item)
        }
    }

    @Test
    fun `test immediate items`() {
        runBlocking(Dispatchers.Default) {
            // Given
            val items = arrayOf(5, 23, 42)
            val flow = flowOf(*items)

            // When
            val subscriber = AssertSubscriber.create<Int>(5)
            val multi = flow.asMulti()
            multi.subscribe().withSubscriber(subscriber)
            val collectedItems = multi.collect().asList().await().indefinitely()

            // Then
            assertThat(collectedItems).containsExactly(*items)
            subscriber.await().assertItems(*items)
        }
    }

    @Test
    fun `test immediate failure`() {
        runBlocking(Dispatchers.Default) {
            // Given
            val flow = flow<Any> {
                error("boom")
            }

            // When
            val subscriber = AssertSubscriber.create<Any>(1)
            flow.asMulti().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.await().assertFailedWith(IllegalStateException::class.java, "boom")
        }
    }

    @Test
    fun `verify that coroutine cancellation result in failure`() {
        runBlocking(Dispatchers.Default) {
            // Given
            val flow = flow<UUID> {
                delay(200)
                emit(UUID.randomUUID())
            }
            val subscriber = AssertSubscriber.create<UUID>(1)

            // When
            runBlocking {
                val job = launch {
                    flow.asMulti().subscribe().withSubscriber(subscriber)
                }
                delay(50)
                job.cancel(CancellationException("abort"))
            }
            Thread.sleep(350)

            // Then
            subscriber.await().assertFailedWith(CancellationException::class.java, "abort")
        }
    }

    @Test
    fun `verify that Flow cancels on Multi subscription cancellation`() {
        runBlocking(Dispatchers.IO) {
            // Given
            val counter = AtomicInteger()
            var exitException: Throwable? = null
            val flow = flow {
                try {
                    while (true) {
                        emit(counter.incrementAndGet())
                    }
                } catch (err: Throwable) {
                    exitException  = err
                }
            }

            // When
            val subscriber = AssertSubscriber.create<Int>(42)
            flow.asMulti().subscribe().withSubscriber(subscriber)
            delay(50)
            subscriber.cancel()
            delay(50)

            // Then
            assertThat(counter.get()).isGreaterThan(0)
            subscriber.assertItems(*(1..42).toList().toTypedArray())
            subscriber.assertNotTerminated()
            assertThat(subscriber.isCancelled).isTrue()
            assertThat(exitException).isNotNull().isInstanceOf(CancellationException::class.java)
        }
    }
}
