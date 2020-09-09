package snippets;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;

public class DroppedExceptionTest {

    @Test
    public void droppedExceptionTest() {
        Logger logger = Logger.getLogger(DroppedExceptionTest.class.getCanonicalName());

        // tag::override-handler[]
        Infrastructure.setDroppedExceptionHandler(err -> logger.log(Level.SEVERE, "Mutiny dropped exception", err));
        // end::override-handler[]

        // tag::code[]
        Cancellable cancellable = Uni.createFrom()
                .emitter(this::emitter)
                .onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
                .subscribe().with(this::onItem, this::onFailure);

        cancellable.cancel();
        // end::code[]
    }

    private void onFailure(Throwable t) {
        // Nothing, the snippet is for pure documentation purposes
    }

    private void onItem(Object o) {
        // Nothing, the snippet is for pure documentation purposes
    }

    private <T> void emitter(UniEmitter<? super T> uniEmitter) {
        // Nothing, the snippet is for pure documentation purposes
    }
}
