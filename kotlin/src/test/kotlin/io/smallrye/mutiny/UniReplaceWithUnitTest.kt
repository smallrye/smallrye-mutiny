package io.smallrye.mutiny

import io.smallrye.mutiny.helpers.test.UniAssertSubscriber
import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class UniReplaceWithUnitTest {

    @Test
    fun `test an Uni holding a value`() {
        // Given
        val uni = Uni.createFrom().item("hold me tight")

        // When
        val subscriber = UniAssertSubscriber.create<Any>()
        uni.replaceWithUnit().subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isSameAs(Unit)
    }

    @Test
    fun `test an Uni holding null`() {
        // Given
        val uni = Uni.createFrom().nullItem<Any>()

        // When
        val subscriber = UniAssertSubscriber.create<Any>()
        uni.replaceWithUnit().subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isSameAs(Unit)
    }

    @Test
    fun `test an Uni that's already Void`() {
        // Given
        val uni = Uni.createFrom().voidItem();

        // When
        val subscriber = UniAssertSubscriber.create<Any>()
        uni.replaceWithUnit().subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitItem().item).isSameAs(Unit)
    }

    @Test
    fun `test a failed Uni`() {
        // Given
        val uni = Uni.createFrom().failure<RuntimeException>(RuntimeException("Kaboom"))

        // When
        val subscriber = UniAssertSubscriber.create<Any>()
        uni.replaceWithUnit().subscribe().withSubscriber(subscriber)

        // Then
        assertThat(subscriber.awaitFailure().failure).hasMessage("Kaboom")
    }
}
