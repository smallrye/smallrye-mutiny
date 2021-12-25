package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat

@ExperimentalCoroutinesApi
class DeferredAsUniTest {

    @Test
    fun `test immediate value`() {
        testBlocking {
            // Given
            val value = UUID.randomUUID()
            val deferred = embeddedScope().async { value }

            // When
            val subscriber = UniAssertSubscriber.create<UUID>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            assertThat(subscriber.awaitItem().item).isEqualTo(value)
        }
    }

    @Test
    fun `test immediate failure`() {
        testBlocking {
            // Given an error produced by a Deferred created in an isolated CoroutineScope
            val deferred = embeddedScope().async { error("kaboom") }

            // When
            val subscriber = UniAssertSubscriber<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            subscriber.awaitFailure().assertFailedWith(IllegalStateException::class.java, "kaboom")
        }
    }

    @Test
    fun `test coroutine cancellation before value is computed`() {
        testBlocking {
            // Given
            val deferred = embeddedScope().async {
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
        testBlocking {
            // Given
            val deferred = embeddedScope().async { null }

            // When
            val subscriber = UniAssertSubscriber.create<Any>()
            deferred.asUni().subscribe().withSubscriber(subscriber)

            // Then
            assertThat(subscriber.awaitItem().item).isNull()
        }
    }

    @Test
    fun `test uni cancellation before deferred completes`() {
        testBlocking {
            // Given
            val deferredLatch = CountDownLatch(1)
            val deferred = embeddedScope().async {
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
            awaitLatch("Uni", uniLatch)
            awaitLatch("Deferred", deferredLatch)
            assertThat(deferred.isCancelled)
                .`as`("Check that Deferred is cancelled")
                .isTrue
        }
    }

    @Test
    fun `test cancelling deferred does not impact siblings`() {
        testBlocking {
            // Start before async.
            val child1Latch = CountDownLatch(1)
            val child1 = launch {
                delay(1000)
            }
            child1.invokeOnCompletion { exception ->
                child1Latch.countDown()
                assertThat(exception).isNull()
            }

            // Start async & cancel it after 100ms.
            val deferredLatch = CountDownLatch(1)
            val deferred = async {
                delay(10_000)
                "Hello, world!"
            }
            deferred.invokeOnCompletion { exception ->
                deferredLatch.countDown()
                assertThat(exception)
                    .isNotNull
                    .isInstanceOf(CancellationException::class.java)
            }

            val uniLatch = CountDownLatch(1)
            val uni = deferred.asUni()
                .onCancellation().invoke { uniLatch.countDown() }

            val subscriber = UniAssertSubscriber.create<String>()
            uni.subscribe().withSubscriber(subscriber)
            subscriber.cancel()

            // Start after cancelling async.
            val child2Latch = CountDownLatch(1)
            val child2 = launch {
                delay(1500)
            }
            child2.invokeOnCompletion { exception ->
                child2Latch.countDown()
                assertThat(exception).isNull()
            }

            withContext(Dispatchers.IO) {
                awaitLatch("Child 1", child1Latch)
                assertThat(child1.isCompleted)
                    .`as`("Check that child 1 completed")
                    .isTrue

                awaitLatch("Uni", uniLatch)
                awaitLatch("Deferred", deferredLatch)
                assertThat(deferred)
                    .`as`("Check that Deferred was set")
                    .isNotNull
                assertThat(deferred.isCancelled)
                    .`as`("Check that Deferred was cancelled")
                    .isTrue
                awaitLatch("Sibling 2", child2Latch)
                assertThat(child2.isCompleted)
                    .`as`("Check that child 2 completed")
                    .isTrue
            }
        }
    }
}

private fun awaitLatch(name: String, latch: CountDownLatch) {
    assertThat(latch.await(2, TimeUnit.SECONDS))
        .`as`("Check that $name completes within 2s")
        .isTrue
}
