package io.smallrye.mutiny.streams.stages;

import org.junit.Test;

import io.smallrye.mutiny.streams.operators.PublisherStage;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

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
        boom.get().subscribe().with(MultiAssertSubscriber.create())
                .assertHasFailedWith(Exception.class, "Boom");
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutError() {
        factory.create(null, () -> null);
    }

    @Test(expected = NullPointerException.class)
    public void createWithoutStage() {
        factory.create(null, null);
    }
}
