package io.smallrye.mutiny.operators.multi.builders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Mockito;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AbstractSubscriber;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.Mocks;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ)
public class MultiFromIterableTest {

    @Test
    public void nullMustBeRejected() {
        assertThrows(IllegalArgumentException.class, () -> Multi.createFrom().iterable(null));
    }

    @Test
    public void testList() {
        Multi<String> multi = Multi.createFrom().iterable(Arrays.asList("one", "two", "three"));
        Subscriber<String> subscriber = Mocks.subscriber();

        multi.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("one");
        verify(subscriber, times(1)).onNext("two");
        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testSwitchFromRequestToUnbounded() {
        Multi<Integer> multi = Multi.createFrom().items(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AssertSubscriber<Integer> subscriber = multi.subscribe()
                .withSubscriber(AssertSubscriber.create(0));

        subscriber.request(1)
                .awaitItems(1)
                .assertItems(1);

        subscriber.request(1)
                .awaitItems(2)
                .assertItems(1, 2);

        subscriber.request(Long.MAX_VALUE)
                .awaitCompletion()
                .assertItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    /**
     * This tests the path that can not optimize based on size so must use setProducer.
     */
    @Test
    public void testWithRawIterable() {
        Iterable<String> it = () -> new Iterator<String>() {

            int i;

            @Override
            public boolean hasNext() {
                return i < 3;
            }

            @Override
            public String next() {
                return String.valueOf(++i);
            }

            @Override
            public void remove() {
            }

        };
        Multi<String> multi = Multi.createFrom().iterable(it);

        Subscriber<String> subscriber = Mocks.subscriber();

        multi.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("1");
        verify(subscriber, times(1)).onNext("2");
        verify(subscriber, times(1)).onNext("3");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void testRequestProtocol() {
        ArrayList<Integer> list = new ArrayList<>(256);
        for (int i = 1; i <= 256 + 1; i++) {
            list.add(i);
        }
        Multi<Integer> f = Multi.createFrom().iterable(list);

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        subscriber.assertHasNotReceivedAnyItem();
        f.subscribe(subscriber);
        subscriber.request(1);
        subscriber.assertItems(1);
        subscriber.request(2);
        subscriber.assertItems(1, 2, 3);
        subscriber.request(3);
        subscriber.assertItems(1, 2, 3, 4, 5, 6);
        subscriber.request(list.size());
        subscriber.assertTerminated();
    }

    @Test
    public void testWithoutBackPressure() {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3, 4, 5));

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        subscriber.assertHasNotReceivedAnyItem();

        subscriber.request(Long.MAX_VALUE); // infinite
        f.subscribe(subscriber);

        subscriber.assertItems(1, 2, 3, 4, 5);
        subscriber.assertTerminated();
    }

    @Test
    public void testThatWeCanSubscribeMultipleTimes() {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3));

        for (int i = 0; i < 10; i++) {
            AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

            f.subscribe(subscriber);

            subscriber.assertItems(1, 2, 3)
                    .assertCompleted();
        }
    }

    @Test
    public void fromIterableRequestOverflow() throws InterruptedException {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3, 4));

        final int expectedCount = 4;
        CountDownLatch latch = new CountDownLatch(expectedCount);

        f.runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe(new Subscriber<Integer>() {
                    Subscription upstream;

                    @Override
                    public void onComplete() {
                        //ignore
                    }

                    @Override
                    public void onError(Throwable e) {
                        throw new RuntimeException(e);
                    }

                    @Override
                    public void onSubscribe(Subscription s) {
                        upstream = s;
                        s.request(2);
                    }

                    @Override
                    public void onNext(Integer t) {
                        latch.countDown();
                        upstream.request(Long.MAX_VALUE - 1);
                    }
                });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void fromEmptyIterableWhenZeroRequestedShouldStillEmitOnCompletedEagerly() {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Multi.createFrom().iterable(Collections.emptyList()).subscribe(new Subscriber<Object>() {

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onSubscribe(Subscription s) {

            }

            @Override
            public void onNext(Object t) {

            }
        });
        assertTrue(completed.get());
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredWithBackpressure() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = () -> new Iterator<Integer>() {

            int count = 1;

            @Override
            public void remove() {
                // ignore
            }

            @Override
            public boolean hasNext() {
                if (count > 1) {
                    called.set(true);
                    return false;
                }
                return true;
            }

            @Override
            public Integer next() {
                return count++;
            }

        };
        Multi.createFrom().iterable(iterable)
                .select().first(1)
                .subscribe().withSubscriber(AssertSubscriber.create(10));
        assertFalse(called.get());
    }

    @Test
    public void doesNotCallIteratorHasNextMoreThanRequiredFastPath() {
        final AtomicBoolean called = new AtomicBoolean(false);
        Iterable<Integer> iterable = () -> new Iterator<Integer>() {

            @Override
            public void remove() {
                // ignore
            }

            int count = 1;

            @Override
            public boolean hasNext() {
                if (count > 1) {
                    called.set(true);
                    return false;
                }
                return true;
            }

            @Override
            public Integer next() {
                return count++;
            }

        };
        Multi.createFrom().iterable(iterable).subscribe(new AbstractSubscriber<Integer>() {

            @Override
            public void onNext(Integer t) {
                // unsubscribe on first emission
                cancel();
            }
        });
        assertFalse(called.get());
    }

    @Test
    public void getIteratorThrows() {
        Iterable<Integer> it = () -> {
            throw new IllegalStateException("BOOM");
        };

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();

        Multi.createFrom().iterable(it).subscribe(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void hasNextThrowsImmediately() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                throw new IllegalStateException("BOOM");
            }

            @Override
            public Integer next() {
                return null;
            }
        };
        AssertSubscriber<Integer> subscriber = AssertSubscriber.create();
        Multi.createFrom().iterable(it).subscribe(subscriber);
        subscriber.assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void hasNextThrowsSecondTimeFastPath() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            int count;

            @Override
            public boolean hasNext() {
                if (++count >= 2) {
                    throw new IllegalStateException("BOOM");
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        };

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().iterable(it).subscribe(subscriber);

        subscriber.assertItems(1)
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void hasNextThrowsSecondTimeSlowPath() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            int count;

            @Override
            public boolean hasNext() {
                if (++count >= 2) {
                    throw new IllegalStateException("BOOM");
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }
        };

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(10);
        Multi.createFrom().iterable(it).subscribe(subscriber);
        subscriber.assertItems(1)
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void nextThrowsFastPath() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                throw new IllegalStateException("BOOM");
            }
        };

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(Long.MAX_VALUE);

        Multi.createFrom().iterable(it).subscribe(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void nextThrowsSlowPath() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                throw new IllegalStateException("BOOM");
            }
        };

        AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().iterable(it).subscribe(subscriber);

        subscriber.assertHasNotReceivedAnyItem()
                .assertFailedWith(IllegalStateException.class, "BOOM");
    }

    @Test
    public void deadOnArrival() {
        Iterable<Integer> it = () -> new Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                // ignored
            }
        };

        AssertSubscriber<Integer> subscriber = new AssertSubscriber<>(5, true);
        Multi.createFrom().iterable(it).subscribe(subscriber);
        subscriber.assertHasNotReceivedAnyItem()
                .assertNotTerminated();
    }

    @Test
    public void hasNextCancels() {
        final AssertSubscriber<Integer> subscriber = AssertSubscriber.create(1);

        Multi.createFrom().iterable(() -> new Iterator<Integer>() {
            int count;

            @Override
            public boolean hasNext() {
                if (++count == 2) {
                    subscriber.cancel();
                }
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        })
                .subscribe(subscriber);

        subscriber.assertItems(1)
                .assertNotTerminated();
    }

}
