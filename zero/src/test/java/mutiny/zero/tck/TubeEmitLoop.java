package mutiny.zero.tck;

import java.util.concurrent.atomic.AtomicLong;

import mutiny.zero.Tube;
import mutiny.zero.internal.Helper;

public interface TubeEmitLoop {

    static void loop(Tube<Long> tube, long tckElements) {
        new Thread(() -> {
            long n = 0L;
            AtomicLong pending = new AtomicLong();
            tube.whenRequested(count -> Helper.add(pending, count));
            while (n < tckElements && !tube.cancelled()) {
                if (pending.get() > 0L) {
                    pending.decrementAndGet();
                    tube.send(n++);
                }
            }
            if (!tube.cancelled()) {
                tube.complete();
            }
        }).start();
    }
}
