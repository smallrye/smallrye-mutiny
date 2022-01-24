package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.AssertSubscriber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.awaitility.Awaitility.await

class FlowAsMultiTest {

    @Test
    fun `test immediate item`() {
        testBlocking {
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
            subscriber.awaitCompletion().assertItems(item)
        }
    }

    @Test
    fun `test immediate items`() {
        testBlocking {
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
            subscriber.awaitCompletion().assertItems(*items)
        }
    }

    @Test
    fun `test immediate failure`() {
        testBlocking {
            // Given
            val flow = flow<Any> {
                error("boom")
            }

            // When
            val subscriber = AssertSubscriber.create<Any>(1)
            flow.asMulti().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.awaitFailure()
                .assertFailedWith(IllegalStateException::class.java, "boom")
        }
    }

    @Test
    fun `verify that coroutine cancellation result in failure`() {
        testBlocking {
            Awaitility.pollInSameThread()
            val ready = AtomicBoolean()

            // Given
            val flow = flow<UUID> {
                ready.set(true)
                delay(200)
                emit(UUID.randomUUID())
            }
            val subscriber = AssertSubscriber.create<UUID>(1)

            // When
            val job = launch {
                flow.asMulti().subscribe().withSubscriber(subscriber)
            }
            await().untilTrue(ready)
            job.cancel(CancellationException("abort"))

            // Then
            subscriber.awaitFailure().assertFailedWith(CancellationException::class.java, "abort")
        }
    }

    @Test
    fun `verify that Flow cancels on Multi subscription cancellation`() {
        testBlocking {
            // Given
            val counter = AtomicInteger()
            var exitException: Throwable? = null
            val flow = flow {
                try {
                    while (true) {
                        emit(counter.incrementAndGet())
                    }
                } catch (err: Throwable) {
                    exitException = err
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
            assertThat(subscriber.isCancelled).isTrue
            assertThat(exitException).isNotNull.isInstanceOf(CancellationException::class.java)
        }
    }

    @Test
    fun `verify that callbackFlow cancels on Multi subscription cancellation`() {
        testBlocking {
            // Given
            val closed = AtomicBoolean(false)
            val flow = callbackFlow<Int> {
                // Real code would set up their callback handler here
                // and call ProducerScope.trySendBlocking or
                // ProducerScope.channel.close() as needed

                // Do nothing to simulate no incoming callbacks

                awaitClose {
                    closed.set(true)
                }
            }

            // When
            val subscriber = AssertSubscriber.create<Int>(42)
            flow.asMulti().subscribe().withSubscriber(subscriber)
            delay(50)
            subscriber.cancel()
            delay(50)

            // Then
            subscriber.assertNotTerminated()
            assertThat(subscriber.isCancelled).isTrue
            assertThat(closed).isTrue
        }
    }
}
