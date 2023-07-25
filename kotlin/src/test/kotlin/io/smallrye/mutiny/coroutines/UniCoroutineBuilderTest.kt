package io.smallrye.mutiny.coroutines

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat

@ExperimentalCoroutinesApi
class UniCoroutineBuilderTest {

    @Test
    fun `test build uni from item`() {
        // Given
        val item = "wrap me plenty"

        // When
        val uni = uni {
            delay(1)
            item
        }
        val subscriber = UniAssertSubscriber.create<String>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isSameAs(item)
    }

    @Test
    fun `test build uni from null`() {
        // Given & When
        val uni = uni {
            delay(1)
            null
        }
        val subscriber = UniAssertSubscriber.create<Any?>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isNull()
    }

    @Test
    fun `test build uni from failure`() {
        // Given & When
        val uni = uni {
            delay(1)
            throw Exception("Kaboom")
        }
        val subscriber = UniAssertSubscriber.create<Any?>()
        uni.subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitFailure().failure).hasMessage("Kaboom")
    }
}
