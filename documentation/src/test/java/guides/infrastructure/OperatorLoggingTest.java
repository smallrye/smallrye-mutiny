package guides.infrastructure;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class OperatorLoggingTest {

    @Test
    public void test() {
        // <set-logger>
        Infrastructure.setOperatorLogger((id, event, value, err) -> {
            Logger logger = LoggerFactory.getLogger(id);
            if (err != null) {
                logger.info(event + "(" + err.getClass() + "(" + err.getMessage() + "))");
            } else {
                if (value != null) {
                    logger.info(event + "(" + value + ")");
                } else {
                    logger.info(event + "()");
                }
            }
        });
        // </set-logger>

        // <log>
        Multi.createFrom().items(1, 2, 3)
                .onItem().transform(n -> n * 10)
                .log()
                .subscribe().with(item -> System.out.println(">>> " + item));
        // </log>
    }
}
