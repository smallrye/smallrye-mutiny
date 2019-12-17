package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniDelegatingSubscriber;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

public class UniInterceptorTest {

    @AfterMethod
    public void cleanup() {
        Infrastructure.clearInterceptors();
    }

    // Test on events

    @Test
    public void testOrdering() {
        UniInterceptor interceptor1 = new UniInterceptor() {
            @Override
            public int ordinal() {
                return 1;
            }
        };

        UniInterceptor interceptor2 = new UniInterceptor() {
            @Override
            public int ordinal() {
                return 2;
            }
        };

        Infrastructure.registerUniInterceptor(interceptor1);
        Infrastructure.registerUniInterceptor(interceptor2);

        assertThat(Infrastructure.getUniInterceptors()).hasSize(2);
        assertThat(Infrastructure.getUniInterceptors().get(0)).isEqualTo(interceptor1);
        assertThat(Infrastructure.getUniInterceptors().get(1)).isEqualTo(interceptor2);

        Infrastructure.clearInterceptors();
        Infrastructure.registerUniInterceptor(interceptor2);
        Infrastructure.registerUniInterceptor(interceptor1);
        assertThat(Infrastructure.getUniInterceptors().get(0)).isEqualTo(interceptor1);
        assertThat(Infrastructure.getUniInterceptors().get(1)).isEqualTo(interceptor2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreationInterception() {
        Infrastructure.registerUniInterceptor(new UniInterceptor() {

            long creationTime = System.nanoTime();

            @Override
            public <T> Uni<T> onUniCreation(Uni<T> uni) {
                return new AbstractUni<T>() {
                    @Override
                    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
                        assertThat(creationTime).isLessThan(System.nanoTime());
                        uni.subscribe().withSubscriber(new UniDelegatingSubscriber(subscriber) {
                            @Override
                            public void onItem(Object item) {
                                super.onItem(((Integer) item) + 1);
                            }
                        });
                    }
                };
            }
        });

        assertThat(Uni.createFrom().item(1).await().indefinitely()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreationInterceptionWithMap() {
        Infrastructure.registerUniInterceptor(new UniInterceptor() {

            long creationTime = System.nanoTime();

            @Override
            public <T> Uni<T> onUniCreation(Uni<T> uni) {
                return new AbstractUni<T>() {
                    @Override
                    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
                        assertThat(creationTime).isLessThan(System.nanoTime());
                        uni.subscribe().withSubscriber(new UniDelegatingSubscriber(subscriber) {
                            @Override
                            public void onItem(Object item) {
                                super.onItem(((Integer) item) + 1);
                            }
                        });
                    }
                };
            }
        });

        assertThat(Uni.createFrom().item(1).map(i -> i + 1).await().indefinitely()).isEqualTo(4);
    }

    @Test
    public void testEventInterceptionOnItem() {
        UniInterceptor interceptor = new UniInterceptor() {
            @Override
            public <T> UniSubscriber<? super T> onSubscription(Uni<T> instance,
                    UniSubscriber<? super T> subscriber) {
                return new UniSubscriber<T>() {
                    @Override
                    public void onSubscribe(UniSubscription subscription) {
                        subscriber.onSubscribe(subscription);
                    }

                    @Override
                    public void onItem(T item) {
                        Integer val = (Integer) item;
                        val = val + 1;
                        //noinspection unchecked
                        subscriber.onItem((T) val);
                    }

                    @Override
                    public void onFailure(Throwable failure) {
                        subscriber.onFailure(failure);
                    }
                };
            }
        };

        Infrastructure.registerUniInterceptor(interceptor);

        int result = Uni.createFrom().item(23).map(i -> i * 2).await().indefinitely();
        assertThat(result).isEqualTo(23 * 2 + 1 + 1 + 1); // 3 subscribers: item, map and the subscriber
    }
}
