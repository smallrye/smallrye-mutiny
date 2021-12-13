package io.smallrye.mutiny.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniDelegatingSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;
import junit5.support.InfrastructureResource;

@ResourceLock(value = InfrastructureResource.NAME, mode = ResourceAccessMode.READ_WRITE)
public class UniInterceptorTest {

    @AfterEach
    public void cleanup() {
        Infrastructure.clearInterceptors();
    }

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

        InfrastructureHelper.registerUniInterceptor(interceptor1);
        InfrastructureHelper.registerUniInterceptor(interceptor2);

        assertThat(InfrastructureHelper.getUniInterceptors()).hasSize(2);
        assertThat(InfrastructureHelper.getUniInterceptors().get(0)).isEqualTo(interceptor1);
        assertThat(InfrastructureHelper.getUniInterceptors().get(1)).isEqualTo(interceptor2);

        Infrastructure.clearInterceptors();
        InfrastructureHelper.registerUniInterceptor(interceptor2);
        InfrastructureHelper.registerUniInterceptor(interceptor1);
        assertThat(InfrastructureHelper.getUniInterceptors().get(0)).isEqualTo(interceptor1);
        assertThat(InfrastructureHelper.getUniInterceptors().get(1)).isEqualTo(interceptor2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreationInterception() {
        InfrastructureHelper.registerUniInterceptor(new UniInterceptor() {

            final long creationTime = System.nanoTime();

            @Override
            public <T> Uni<T> onUniCreation(Uni<T> uni) {
                return new AbstractUni<T>() {
                    @Override
                    public void subscribe(UniSubscriber<? super T> subscriber) {
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
        InfrastructureHelper.registerUniInterceptor(new UniInterceptor() {

            final long creationTime = System.nanoTime();

            @Override
            public <T> Uni<T> onUniCreation(Uni<T> uni) {
                return new AbstractUni<T>() {
                    @Override
                    public void subscribe(UniSubscriber<? super T> subscriber) {
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
                    public Context context() {
                        return Context.empty();
                    }

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

        InfrastructureHelper.registerUniInterceptor(interceptor);

        int result = Uni.createFrom().item(23).map(i -> i * 2).await().indefinitely();
        assertThat(result).isEqualTo(23 * 2 + 1 + 1 + 1); // 3 subscribers: item, map and the subscriber
    }

    @Test
    public void testDefaultOrdinal() {
        UniInterceptor itcp = new UniInterceptor() {
            // do nothing
        };

        assertThat(itcp.ordinal()).isEqualTo(UniInterceptor.DEFAULT_ORDINAL);

        itcp = new UniInterceptor() {
            @Override
            public int ordinal() {
                return 25;
            }
        };

        assertThat(itcp.ordinal()).isEqualTo(25);
    }
}
