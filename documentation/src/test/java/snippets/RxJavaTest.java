package snippets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import io.smallrye.mutiny.converters.uni.UniRxConverters;

public class RxJavaTest {

    @Test
    public void createUniFromRx() {
        // tag::uni-create[]
        Completable completable = Completable.complete();
        Single<String> single = Single.just("hello");
        Maybe<String> maybe = Maybe.just("hello");
        Maybe<String> emptyMaybe = Maybe.empty();
        Observable<String> observable = Observable.fromArray("a", "b", "c");
        Flowable<String> flowable = Flowable.fromArray("a", "b", "c");

        Uni<Void> uniFromCompletable = Uni.createFrom().converter(UniRxConverters.fromCompletable(), completable);
        Uni<String> uniFromSingle = Uni.createFrom().converter(UniRxConverters.fromSingle(), single);
        Uni<String> uniFromMaybe = Uni.createFrom().converter(UniRxConverters.fromMaybe(), maybe);
        Uni<String> uniFromEmptyMaybe = Uni.createFrom().converter(UniRxConverters.fromMaybe(), emptyMaybe);
        Uni<String> uniFromObservable = Uni.createFrom().converter(UniRxConverters.fromObservable(), observable);
        Uni<String> uniFromFlowable = Uni.createFrom().converter(UniRxConverters.fromFlowable(), flowable);
        Uni<String> uniFromPublisher = Uni.createFrom().publisher(flowable);

        // end::uni-create[]

        assertThat(uniFromCompletable.await().indefinitely()).isNull();
        assertThat(uniFromSingle.await().indefinitely()).isEqualTo("hello");
        assertThat(uniFromMaybe.await().indefinitely()).isEqualTo("hello");
        assertThat(uniFromEmptyMaybe.await().indefinitely()).isNull();
        assertThat(uniFromObservable.await().indefinitely()).isEqualTo("a");
        assertThat(uniFromFlowable.await().indefinitely()).isEqualTo("a");
        assertThat(uniFromPublisher.await().indefinitely()).isEqualTo("a");
    }

    @Test
    public void createMultiFromRx() {
        // tag::multi-create[]
        Completable completable = Completable.complete();
        Single<String> single = Single.just("hello");
        Maybe<String> maybe = Maybe.just("hello");
        Maybe<String> emptyMaybe = Maybe.empty();
        Observable<String> observable = Observable.fromArray("a", "b", "c");
        Flowable<String> flowable = Flowable.fromArray("a", "b", "c");

        Multi<Void> multiFromCompletable = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), completable);
        Multi<String> multiFromSingle = Multi.createFrom().converter(MultiRxConverters.fromSingle(), single);
        Multi<String> multiFromMaybe = Multi.createFrom().converter(MultiRxConverters.fromMaybe(), maybe);
        Multi<String> multiFromEmptyMaybe = Multi.createFrom().converter(MultiRxConverters.fromMaybe(), emptyMaybe);
        Multi<String> multiFromObservable = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), observable);
        Multi<String> multiFromFlowable = Multi.createFrom().converter(MultiRxConverters.fromFlowable(), flowable);
        Multi<String> multiFromPublisher = Multi.createFrom().publisher(flowable);
        // end::multi-create[]

        assertThat(multiFromCompletable.collectItems().first().await().indefinitely()).isNull();
        assertThat(multiFromSingle.collectItems().first().await().indefinitely()).isEqualTo("hello");
        assertThat(multiFromMaybe.collectItems().first().await().indefinitely()).isEqualTo("hello");
        assertThat(multiFromEmptyMaybe.collectItems().first().await().indefinitely()).isNull();
        assertThat(multiFromObservable.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
        assertThat(multiFromFlowable.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
        assertThat(multiFromPublisher.collectItems().asList().await().indefinitely()).containsExactly("a", "b", "c");
    }

    @Test
    public void uniExportToRx() {
        Uni<String> uni = Uni.createFrom().item("hello");
        // tag::uni-export[]
        Completable completable = uni.convert().with(UniRxConverters.toCompletable());
        Single<Optional<String>> single = uni.convert().with(UniRxConverters.toSingle());
        Single<String> single2 = uni.convert().with(UniRxConverters.toSingle().failOnNull());
        Maybe<String> maybe = uni.convert().with(UniRxConverters.toMaybe());
        Observable<String> observable = uni.convert().with(UniRxConverters.toObservable());
        Flowable<String> flowable = uni.convert().with(UniRxConverters.toFlowable());
        // end::uni-export[]

        completable.test().assertComplete();
        single.test().assertValue(o -> o.isPresent() && o.get().equals("hello"));
        single2.test().assertValue("hello");
        maybe.test().assertValue("hello");
        observable.test().assertValue("hello").assertComplete();
        flowable.test().assertValue("hello").assertComplete();
    }

    @Test
    public void multiExportToRx() {
        Multi<String> multi = Multi.createFrom().items("hello", "bonjour");
        // tag::multi-export[]
        Completable completable = multi.convert().with(MultiRxConverters.toCompletable());
        Single<Optional<String>> single = multi.convert().with(MultiRxConverters.toSingle());
        Single<String> single2 = multi.convert().with(MultiRxConverters
                .toSingle().onEmptyThrow(() -> new Exception("D'oh!")));
        Maybe<String> maybe = multi.convert().with(MultiRxConverters.toMaybe());
        Observable<String> observable = multi.convert().with(MultiRxConverters.toObservable());
        Flowable<String> flowable = multi.convert().with(MultiRxConverters.toFlowable());
        // end::multi-export[]

        completable.test().assertComplete();
        single.test().assertValue(o -> o.isPresent() && o.get().equals("hello"));
        single2.test().assertValue("hello");
        maybe.test().assertValue("hello");
        observable.test().assertValues("hello", "bonjour").assertComplete();
        flowable.test().assertValues("hello", "bonjour").assertComplete();
    }
}
