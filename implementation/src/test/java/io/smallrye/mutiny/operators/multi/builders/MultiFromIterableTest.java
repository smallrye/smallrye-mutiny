package io.smallrye.mutiny.operators.multi.builders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.test.AbstractSubscriber;
import io.smallrye.mutiny.test.Mocks;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiFromIterableTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullMustBeRejected() {
        Multi.createFrom().iterable(null);
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        ts.assertHasNotReceivedAnyItem();
        f.subscribe(ts);
        ts.request(1);
        ts.assertReceived(1);
        ts.request(2);
        ts.assertReceived(1, 2, 3);
        ts.request(3);
        ts.assertReceived(1, 2, 3, 4, 5, 6);
        ts.request(list.size());
        ts.assertTerminated();
    }

    @Test
    public void testWithoutBackPressure() {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3, 4, 5));

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        ts.assertHasNotReceivedAnyItem();

        ts.request(Long.MAX_VALUE); // infinite
        f.subscribe(ts);

        ts.assertReceived(1, 2, 3, 4, 5);
        ts.assertTerminated();
    }

    @Test
    public void testThatWeCanSubscribeMultipleTimes() {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3));

        for (int i = 0; i < 10; i++) {
            MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(Long.MAX_VALUE);

            f.subscribe(ts);

            ts.assertReceived(1, 2, 3)
                    .assertCompletedSuccessfully();
        }
    }

    @SuppressWarnings("SubscriberImplementation")
    @Test
    public void fromIterableRequestOverflow() throws InterruptedException {
        Multi<Integer> f = Multi.createFrom().iterable(Arrays.asList(1, 2, 3, 4));

        final int expectedCount = 4;
        CountDownLatch latch = new CountDownLatch(expectedCount);

        f.subscribeOn(Infrastructure.getDefaultExecutor())
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

    @SuppressWarnings("SubscriberImplementation")
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
                .transform().byTakingFirstItems(1)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(10));
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();

        Multi.createFrom().iterable(it).subscribe(ts);

        ts.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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
        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create();
        Multi.createFrom().iterable(it).subscribe(ts);
        ts.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(10);

        Multi.createFrom().iterable(it).subscribe(ts);

        ts.assertReceived(1)
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(10);
        Multi.createFrom().iterable(it).subscribe(ts);
        ts.assertReceived(1)
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().iterable(it).subscribe(ts);

        ts.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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

        MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().iterable(it).subscribe(ts);

        ts.assertHasNotReceivedAnyItem()
                .assertHasFailedWith(IllegalStateException.class, "BOOM");
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

        MultiAssertSubscriber<Integer> ts = new MultiAssertSubscriber<>(5, true);
        Multi.createFrom().iterable(it).subscribe(ts);
        ts.assertHasNotReceivedAnyItem()
                .assertNotTerminated();
    }

    @Test
    public void hasNextCancels() {
        final MultiAssertSubscriber<Integer> ts = MultiAssertSubscriber.create(1);

        Multi.createFrom().iterable(() -> new Iterator<Integer>() {
            int count;

            @Override
            public boolean hasNext() {
                if (++count == 2) {
                    ts.cancel();
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
                .subscribe(ts);

        ts.assertReceived(1)
                .assertNotTerminated();
    }

}
