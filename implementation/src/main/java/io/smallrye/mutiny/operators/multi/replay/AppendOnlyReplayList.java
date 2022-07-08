package io.smallrye.mutiny.operators.multi.replay;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

/*
 * Replay is being captured using a custom linked list, while consumers can make progress using cursors.
 *
 * The "start" depends on the replay semantics:
 * - zero for unbounded replays,
 * - the last n elements before the tail for bounded replays.
 *
 * From there each cursor (1 per subscriber) can make progress at its own pace.
 *
 * The code assumes reactive streams semantics, especially that there are no concurrent appends because of
 * serial events.
 *
 * Bounded replays shall have earlier cells before the head be eventually garbage collected as there are only forward
 * references.
 */
public class AppendOnlyReplayList {

    public class Cursor {

        private Cell current = SENTINEL_EMPTY;
        private boolean start = true;
        private boolean currentHasBeenRead = false;

        public boolean hasNext() {
            if (current == SENTINEL_EMPTY) {
                Cell currentHead = head;
                if (currentHead != SENTINEL_EMPTY) {
                    current = currentHead;
                    return true;
                } else {
                    return false;
                }
            } else if (!currentHasBeenRead) {
                return true;
            } else {
                return current.next != SENTINEL_END;
            }
        }

        public void moveToNext() {
            if (start) {
                start = false;
                return;
            }
            assert current.next != SENTINEL_END;
            current = current.next;
            currentHasBeenRead = false;
        }

        public Object read() {
            currentHasBeenRead = true;
            return current.value;
        }

        public boolean hasReachedCompletion() {
            return current.value instanceof Completion;
        }

        public boolean hasReachedFailure() {
            return current.value instanceof Failure;
        }

        public Throwable readFailure() {
            currentHasBeenRead = true;
            return ((Failure) current.value).failure;
        }

        public void readCompletion() {
            currentHasBeenRead = true;
        }
    }

    private static abstract class Terminal {

    }

    private static final class Completion extends Terminal {

    }

    private static final class Failure extends Terminal {
        final Throwable failure;

        Failure(Throwable failure) {
            this.failure = failure;
        }
    }

    private static class Cell {
        final Object value;
        volatile Cell next;

        Cell(Object value, Cell next) {
            this.value = value;
            this.next = next;
        }
    }

    private static final Cell SENTINEL_END = new Cell(null, null);
    private static final Cell SENTINEL_EMPTY = new Cell(null, SENTINEL_END);

    private final long itemsToReplay;
    private long numberOfItemsRecorded = 0L;
    private volatile Cell head = SENTINEL_EMPTY;
    private volatile Cell tail = SENTINEL_EMPTY;

    public AppendOnlyReplayList(long numberOfItemsToReplay) {
        this(numberOfItemsToReplay, null);
    }

    public AppendOnlyReplayList(long numberOfItemsToReplay, Iterable<?> seed) {
        assert numberOfItemsToReplay > 0;
        this.itemsToReplay = numberOfItemsToReplay;
        if (seed != null) {
            seed.forEach(this::push);
        }
    }

    public void push(Object item) {
        assert !(tail.value instanceof Terminal);
        Cell newCell = new Cell(nonNull(item, "item"), SENTINEL_END);
        if (head == SENTINEL_EMPTY) {
            head = newCell;
        } else {
            tail.next = newCell;
        }
        tail = newCell;
        if (itemsToReplay != Long.MAX_VALUE && !(item instanceof Terminal)) {
            numberOfItemsRecorded++;
            if (numberOfItemsRecorded > itemsToReplay) {
                synchronized (this) {
                    head = head.next;
                }
            }
        }
    }

    public void pushFailure(Throwable failure) {
        push(new Failure(failure));
    }

    public void pushCompletion() {
        push(new Completion());
    }

    public Cursor newCursor() {
        return new Cursor();
    }
}
