package guides.infrastructure;

import guides.extension.SystemOut;
import guides.extension.SystemOutCaptureExtension;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.Cancellable;
import io.smallrye.mutiny.subscription.UniEmitter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ExtendWith(SystemOutCaptureExtension.class)
public class DroppedExceptionTest {

    @Test
    public void droppedExceptionTest(SystemOut out) {
        // <override-handler>
        Infrastructure.setDroppedExceptionHandler(err ->
                log(Level.SEVERE, "Mutiny dropped exception", err)
        );
        // </override-handler>

        // <code>
        Cancellable cancellable = Uni.createFrom()
                .emitter(this::emitter)
                .onCancellation().call(() -> Uni.createFrom().failure(new IOException("boom")))
                .subscribe().with(this::onItem, this::onFailure);

        cancellable.cancel();
        // </code>
        Assertions.assertThat(out.get()).contains("Mutiny dropped exception");
    }

    private void log(Level severe, String message, Throwable err) {
        System.out.println(severe.getName() + ":" + message + " " + err.getMessage());
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
