package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test

@ExperimentalCoroutinesApi
class DeferredAsUniTest {

    @Test
    fun `test immediate value`() {
        runBlocking {
            // Given
            val value = UUID.randomUUID()
            val deferred = GlobalScope.async { value }

            // When
            val subscriber = UniAssertSubscriber.create<UUID>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.await().assertItem(value)
        }
    }

    @Test
    fun `test immediate failure`() {
        runBlocking {
            // Given
            val deferred = GlobalScope.async { error("kaboom") }

            // When
            val subscriber = UniAssertSubscriber<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.await().assertFailedWith(IllegalStateException::class.java, "kaboom")
        }
    }

    @Test
    fun `test coroutine cancellation before value is computed`() {
        runBlocking {
            // Given
            val deferred = GlobalScope.async {
                delay(250)
                UUID.randomUUID()
            }

            // When
            val subscriber = UniAssertSubscriber.create<UUID>()
            deferred.asUni().subscribe().withSubscriber(subscriber)
            deferred.cancel(CancellationException("abort"))

            // Then
            subscriber.await().assertFailedWith(CancellationException::class.java, "abort")
        }
    }

    @Test
    fun `test null item`() {
        runBlocking {
            // Given
            val deferred = GlobalScope.async { null }

            // When
            val subscriber = UniAssertSubscriber.create<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.await().assertItem(null)
        }
    }
}
