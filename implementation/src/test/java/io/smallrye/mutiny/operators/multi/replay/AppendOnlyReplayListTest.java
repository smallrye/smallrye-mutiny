package io.smallrye.mutiny.operators.multi.replay;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class AppendOnlyReplayListTest {

    private final Random random = new Random();

    @Test
    void checkReadyAtStart() {
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(Long.MAX_VALUE);
        AppendOnlyReplayList.Cursor cursor = replayList.newCursor();

        assertThat(cursor.hasNext()).isFalse();
        replayList.push("foo");
        replayList.push("bar");
        assertThat(cursor.hasNext()).isTrue();
        cursor.moveToNext();
        assertThat(cursor.read()).isEqualTo("foo");
        assertThat(cursor.hasNext()).isTrue();
        cursor.moveToNext();
        assertThat(cursor.hasNext()).isTrue();
        assertThat(cursor.read()).isEqualTo("bar");
        assertThat(cursor.hasNext()).isFalse();
    }

    @Test
    void pushSomeItemsAndComplete() {
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(Long.MAX_VALUE);
        ArrayList<Integer> reference = new ArrayList<>();

        AppendOnlyReplayList.Cursor firstCursor = replayList.newCursor();
        assertThat(firstCursor.hasNext()).isFalse();
        for (int i = 0; i < 20; i++) {
            replayList.push(i);
            reference.add(i);
        }
        replayList.pushCompletion();
        AppendOnlyReplayList.Cursor secondCursor = replayList.newCursor();

        checkCompletedWithAllItems(reference, firstCursor);
        checkCompletedWithAllItems(reference, secondCursor);
    }

    private void checkCompletedWithAllItems(ArrayList<Integer> reference, AppendOnlyReplayList.Cursor cursor) {
        ArrayList<Integer> proof = new ArrayList<>();
        assertThat(cursor.hasNext()).isTrue();
        while (cursor.hasNext()) {
            cursor.moveToNext();
            if (cursor.hasReachedCompletion()) {
                cursor.readCompletion();
                assumeFalse(cursor.hasNext());
                break;
            }
            assumeFalse(cursor.hasReachedFailure());
            proof.add((Integer) cursor.read());
        }
        assertThat(proof).isEqualTo(reference);
    }

    @Test
    void pushSomeItemsAndFail() {
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(Long.MAX_VALUE);
        ArrayList<Integer> reference = new ArrayList<>();

        AppendOnlyReplayList.Cursor firstCursor = replayList.newCursor();
        for (int i = 0; i < 20; i++) {
            replayList.push(i);
            reference.add(i);
        }
        replayList.pushFailure(new IOException("woops"));
        AppendOnlyReplayList.Cursor secondCursor = replayList.newCursor();

        checkFailedWithAllItems(reference, firstCursor, IOException.class, "woops");
        checkFailedWithAllItems(reference, secondCursor, IOException.class, "woops");
    }

    private void checkFailedWithAllItems(ArrayList<Integer> reference, AppendOnlyReplayList.Cursor cursor, Class<?> failureType,
            String failureMessage) {
        ArrayList<Integer> proof = new ArrayList<>();
        assertThat(cursor.hasNext()).isTrue();
        while (cursor.hasNext()) {
            cursor.moveToNext();
            if (cursor.hasReachedFailure()) {
                assertThat(cursor.readFailure()).isInstanceOf(failureType).hasMessage(failureMessage);
                assumeFalse(cursor.hasNext());
                break;
            }
            assumeFalse(cursor.hasReachedCompletion());
            proof.add((Integer) cursor.read());
        }
        assertThat(proof).isEqualTo(reference);
        assertThat(cursor.hasNext()).isFalse();
        assertThat(cursor.hasReachedFailure()).isTrue();

    }

    @Test
    void boundedReplay() {
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(3);
        replayList.push(1);
        replayList.push(2);

        AppendOnlyReplayList.Cursor firstCursor = replayList.newCursor();
        assertThat(firstCursor.hasNext()).isTrue();
        firstCursor.moveToNext();
        assertThat(firstCursor.read()).isEqualTo(1);
        assertThat(firstCursor.hasNext()).isTrue();
        firstCursor.moveToNext();
        assertThat(firstCursor.read()).isEqualTo(2);
        assertThat(firstCursor.hasNext()).isFalse();

        AppendOnlyReplayList.Cursor secondCursor = replayList.newCursor();
        replayList.push(3);
        replayList.push(4);
        replayList.push(5);

        assertThat(secondCursor.hasNext()).isTrue();
        secondCursor.moveToNext();
        assertThat(secondCursor.read()).isEqualTo(3);
        secondCursor.moveToNext();
        assertThat(secondCursor.read()).isEqualTo(4);
        secondCursor.moveToNext();
        assertThat(secondCursor.read()).isEqualTo(5);
        assertThat(secondCursor.hasNext()).isFalse();

        assertThat(firstCursor.hasNext()).isTrue();
        firstCursor.moveToNext();
        assertThat(firstCursor.read()).isEqualTo(3);
        firstCursor.moveToNext();
        assertThat(firstCursor.read()).isEqualTo(4);

        replayList.push(6);
        replayList.pushFailure(new IOException("boom"));

        AppendOnlyReplayList.Cursor lateCursor = replayList.newCursor();
        assertThat(lateCursor.hasNext()).isTrue();
        lateCursor.moveToNext();
        assertThat(lateCursor.read()).isEqualTo(4);
        assertThat(lateCursor.hasNext()).isTrue();
        lateCursor.moveToNext();
        assertThat(lateCursor.read()).isEqualTo(5);
        assertThat(lateCursor.hasNext()).isTrue();
        lateCursor.moveToNext();
        assertThat(lateCursor.read()).isEqualTo(6);
        assertThat(lateCursor.hasNext()).isTrue();
        lateCursor.moveToNext();
        assertThat(lateCursor.hasReachedFailure()).isTrue();
        assertThat(lateCursor.hasNext()).isTrue();
        assertThat(lateCursor.readFailure()).isInstanceOf(IOException.class).hasMessage("boom");
        assertThat(lateCursor.hasNext()).isFalse();
    }

    @RepeatedTest(2)
    void concurrencySanityChecks() {
        final long N_CONSUMERS = 4L;
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(256);
        AtomicBoolean stop = new AtomicBoolean();
        AtomicLong counter = new AtomicLong();
        AtomicLong success = new AtomicLong();
        ConcurrentLinkedDeque<String> problems = new ConcurrentLinkedDeque<>();
        ExecutorService pool = Executors.newCachedThreadPool();

        pool.submit(() -> {
            randomSleep();
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 5000L) {
                for (int i = 0; i < 5000; i++) {
                    replayList.push(counter.getAndIncrement());
                }
            }
            stop.set(true);
        });

        for (int i = 0; i < N_CONSUMERS; i++) {
            pool.submit(() -> {
                AppendOnlyReplayList.Cursor cursor = replayList.newCursor();
                randomSleep();
                while (!cursor.hasNext()) {
                    // await
                }
                cursor.moveToNext();
                long previous = (long) cursor.read();
                while (!stop.get()) {
                    if (!cursor.hasNext()) {
                        continue;
                    }
                    cursor.moveToNext();
                    long current = (long) cursor.read();
                    if (current != previous + 1) {
                        problems.add("Broken sequence " + previous + " -> " + current);
                        return;
                    }
                    previous = current;
                    success.incrementAndGet();
                }
            });
        }

        await().untilTrue(stop);
        pool.shutdownNow();
        assertThat(problems).isEmpty();
        assertThat(success.get() / N_CONSUMERS).isLessThanOrEqualTo(counter.get());
    }

    private void randomSleep() {
        try {
            Thread.sleep(250 + random.nextInt(250));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    void seedUnbounded() {
        List<Integer> seed = IntStream.range(1, 9).boxed().collect(Collectors.toList());
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(Long.MAX_VALUE, seed);
        replayList.push(9);
        replayList.push(10);
        replayList.pushCompletion();

        List<Integer> expected = IntStream.range(1, 11).boxed().collect(Collectors.toList());
        ArrayList<Integer> check = new ArrayList<>();
        AppendOnlyReplayList.Cursor cursor = replayList.newCursor();
        while (cursor.hasNext()) {
            cursor.moveToNext();
            if (cursor.hasReachedCompletion()) {
                break;
            }
            check.add((Integer) cursor.read());
        }

        assertThat(cursor.hasReachedCompletion()).isTrue();
        assertThat(check).isEqualTo(expected);
    }

    @Test
    void seedBounded() {
        List<Integer> seed = IntStream.range(1, 9).boxed().collect(Collectors.toList());
        AppendOnlyReplayList replayList = new AppendOnlyReplayList(4, seed);
        replayList.push(9);
        replayList.push(10);
        replayList.pushCompletion();

        List<Integer> expected = Arrays.asList(7, 8, 9, 10);
        ArrayList<Integer> check = new ArrayList<>();
        AppendOnlyReplayList.Cursor cursor = replayList.newCursor();
        while (cursor.hasNext()) {
            cursor.moveToNext();
            if (cursor.hasReachedCompletion()) {
                break;
            }
            check.add((Integer) cursor.read());
        }

        assertThat(cursor.hasReachedCompletion()).isTrue();
        assertThat(check).isEqualTo(expected);
    }

    @Test
    void forbidNull() {
        assertThatThrownBy(() -> {
            List<String> seed = Arrays.asList("foo", "bar", null);
            AppendOnlyReplayList replayList = new AppendOnlyReplayList(Long.MAX_VALUE, seed);
            AppendOnlyReplayList.Cursor cursor = replayList.newCursor();
            while (cursor.hasNext()) {
                cursor.moveToNext();
                cursor.read();
            }
        }).isInstanceOf(IllegalArgumentException.class).hasMessage("`item` must not be `null`");
    }
}
