package io.smallrye.mutiny.jakarta.streams.stages;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.jakarta.streams.operators.PublisherStage;

/**
 * Checks the behavior of {@link FailedPublisherStageFactory}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FailedPublisherStageFactoryTest extends StageTestBase {

    private final FailedPublisherStageFactory factory = new FailedPublisherStageFactory();

    @Test
    public void createWithError() {
        Exception failure = new Exception("Boom");
        PublisherStage<Object> boom = factory.create(null, () -> failure);
        boom.get().subscribe().withSubscriber(AssertSubscriber.create())
                .assertFailedWith(Exception.class, "Boom");
    }

    @Test
    public void createWithoutStage() {
        assertThrows(NullPointerException.class, () -> factory.create(null, null));
    }

    @Test
    public void createWithoutError() {
        assertThrows(NullPointerException.class, () -> factory.create(null, () -> null));
    }

}
