package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test

@ExperimentalCoroutinesApi
class DeferredAsUniTest {

    @Test
    fun `test immediate value`() {
        runBlocking {
            // Given
            val value = UUID.randomUUID()
            val deferred = async { value }

            // When
            val subscriber = UniAssertSubscriber.create<UUID>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            assertThat(subscriber.awaitItem().item).isEqualTo(value)
        }
    }

    @Test
    fun `test immediate failure`() {
        runBlocking {
            // Given
            val deferred = async { error("kaboom") }

            // When
            val subscriber = UniAssertSubscriber<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.awaitFailure().assertFailedWith(IllegalStateException::class.java, "kaboom")
        }
    }

    @Test
    fun `test coroutine cancellation before value is computed`() {
        runBlocking {
            // Given
            val deferred = async {
                delay(250)
                UUID.randomUUID()
            }

            // When
            val subscriber = UniAssertSubscriber.create<UUID>()
            deferred.asUni().subscribe().withSubscriber(subscriber)
            deferred.cancel(CancellationException("abort"))

            // Then
            subscriber.awaitFailure().assertFailedWith(CancellationException::class.java, "abort")
        }
    }

    @Test
    fun `test null item`() {
        runBlocking {
            // Given
            val deferred = async { null }

            // When
            val subscriber = UniAssertSubscriber.create<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            assertThat(subscriber.awaitItem().item).isNull()
        }
    }

    @Test
    fun `test uni cancellation before deferred completes`() {
        runBlocking {
            // Given
            val deferredLatch = CountDownLatch(1)
            val deferred = async {
                delay(10_000) // simulate long running job
                "Hello, World!"
            }
            deferred.invokeOnCompletion {
                deferredLatch.countDown()
            }
            val uniLatch = CountDownLatch(1)
            val uni = deferred.asUni()
                .onCancellation().invoke { uniLatch.countDown() }

            // When
            val subscriber = UniAssertSubscriber.create<String>()
            uni.subscribe().withSubscriber(subscriber)
            subscriber.cancel()

            // Then
            assertThat(uniLatch.await(2, TimeUnit.SECONDS))
                .`as`("Check that uni is cancelled within 2 seconds")
                .isTrue()
            assertThat(deferredLatch.await(2, TimeUnit.SECONDS))
                .`as`("Check that deferred is cancelled within 2 seconds")
                .isTrue()
            assertThat(deferred.isCancelled)
                .`as`("Check that Deferred is cancelled")
                .isTrue()
        }
    }
}
