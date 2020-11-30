package guides.integration;

import io.reactivex.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
// tag::rx-import[]
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import io.smallrye.mutiny.converters.uni.UniRxConverters;
// end::rx-import[]
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class RxJavaTest<T> {

    @Test
    public void testMultiCreation() {
        Observable<T> observable = getObservable();
        Flowable<T> flowable = getFlowable();

        // tag::rx-multi-create-observable[]
        Multi<T> multiFromObservable = Multi.createFrom()
                .converter(MultiRxConverters.fromObservable(), observable);
        // end::rx-multi-create-observable[]

        // tag::rx-multi-create-flowable[]
        Multi<T> multiFromFlowable = Multi.createFrom().publisher(flowable);
        // end::rx-multi-create-flowable[]

        List<String> list = multiFromObservable
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("a", "b", "c");

        list = multiFromFlowable
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("a", "b", "c");

        Completable completable = getCompletable();
        Single<T> single = getSingle();
        Maybe<T> maybe = getMaybe();
        Maybe<T> emptyMaybe = getEmptyMaybe();

        // tag::rx-multi-create-single[]
        Multi<Void> multiFromCompletable = Multi.createFrom()
                .converter(MultiRxConverters.fromCompletable(), completable);
        Multi<T> multiFromSingle = Multi.createFrom()
                .converter(MultiRxConverters.fromSingle(), single);
        Multi<T> multiFromMaybe = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), maybe);
        Multi<T> multiFromEmptyMaybe = Multi.createFrom()
                .converter(MultiRxConverters.fromMaybe(), emptyMaybe);
        // end::rx-multi-create-single[]

        list = multiFromCompletable
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).isEmpty();

        list = multiFromSingle
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("a");

        list = multiFromMaybe
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).containsExactly("a");

        list = multiFromEmptyMaybe
                .onItem().transform(Object::toString)
                .collectItems().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testUniCreation() {
        Observable<T> observable = getObservable();
        Flowable<T> flowable = getFlowable();

        // tag::rx-uni-create-observable[]
        Uni<T> uniFromObservable = Uni.createFrom().converter(
                UniRxConverters.fromObservable(), observable);
        // end::rx-uni-create-observable[]

        // tag::rx-uni-create-flowable[]
        Uni<T> uniFromFlowable = Uni.createFrom().publisher(flowable);
        // end::rx-uni-create-flowable[]

        String s = uniFromFlowable
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");

        s = uniFromObservable
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");

        Completable completable = getCompletable();
        Single<T> single = getSingle();
        Maybe<T> maybe = getMaybe();
        Maybe<T> emptyMaybe = getEmptyMaybe();

        // tag::rx-uni-create-single[]
        Uni<Void> multiFromCompletable = Uni.createFrom()
                .converter(UniRxConverters.fromCompletable(), completable);
        Uni<T> multiFromSingle = Uni.createFrom()
                .converter(UniRxConverters.fromSingle(), single);
        Uni<T> multiFromMaybe = Uni.createFrom()
                .converter(UniRxConverters.fromMaybe(), maybe);
        Uni<T> multiFromEmptyMaybe = Uni.createFrom()
                .converter(UniRxConverters.fromMaybe(), emptyMaybe);
        // end::rx-uni-create-single[]

        Void v = multiFromCompletable
                .await().indefinitely();
        assertThat(v).isNull();

        s = multiFromSingle
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");

        s = multiFromMaybe
                .onItem().transform(Object::toString)
                .await().indefinitely();
        assertThat(s).isEqualTo("a");

        s = multiFromEmptyMaybe
                .onItem().castTo(String.class)
                .await().indefinitely();
        assertThat(s).isNull();
    }

    @Test
    public void testCreatingRxFromMulti() {
        Multi<T> multi = getMulti();

        // tag::create-rx-from-multi[]
        Completable completable = multi.convert()
                .with(MultiRxConverters.toCompletable());
        Single<Optional<T>> single = multi.convert()
                .with(MultiRxConverters.toSingle());
        Single<T> single2 = multi.convert()
                .with(MultiRxConverters
                        .toSingle().onEmptyThrow(() -> new Exception("D'oh!")));
        Maybe<T> maybe = multi.convert()
                .with(MultiRxConverters.toMaybe());
        Observable<T> observable = multi.convert()
                .with(MultiRxConverters.toObservable());
        Flowable<T> flowable = multi.convert()
                .with(MultiRxConverters.toFlowable());
        // end::create-rx-from-multi[]

        completable.blockingAwait();
        assertThat(single.blockingGet()).contains((T) "a");
        assertThat(single2.blockingGet()).isEqualTo("a");
        assertThat(maybe.blockingGet()).isEqualTo("a");
        assertThat(observable.blockingIterable()).containsExactly((T) "a", (T) "b", (T) "c");
        assertThat(flowable.blockingIterable()).containsExactly((T) "a", (T) "b", (T) "c");
    }

    @Test
    public void testCreatingRxFromUni() {
        Uni<T> uni = getUni();

        // tag::create-rx-from-uni[]
        Completable completable = uni.convert().with(UniRxConverters.toCompletable());
        Single<Optional<T>> single = uni.convert().with(UniRxConverters.toSingle());
        Single<T> single2 = uni.convert().with(UniRxConverters.toSingle().failOnNull());
        Maybe<T> maybe = uni.convert().with(UniRxConverters.toMaybe());
        Observable<T> observable = uni.convert().with(UniRxConverters.toObservable());
        Flowable<T> flowable = uni.convert().with(UniRxConverters.toFlowable());
        // end::create-rx-from-uni[]

        completable.blockingAwait();
        assertThat(single.blockingGet()).contains((T) "a");
        assertThat(single2.blockingGet()).isEqualTo("a");
        assertThat(maybe.blockingGet()).isEqualTo("a");
        assertThat(observable.blockingIterable()).containsExactly((T) "a");
        assertThat(flowable.blockingIterable()).containsExactly((T) "a");
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
        Completable completable = multi.convert()
                .with(MultiRxConverters.toCompletable());
        Single<Optional<String>> single = multi.convert()
                .with(MultiRxConverters.toSingle());
        Single<String> single2 = multi.convert()
                .with(MultiRxConverters.toSingle().onEmptyThrow(() -> new Exception("D'oh!")));
        Maybe<String> maybe = multi.convert()
                .with(MultiRxConverters.toMaybe());
        Observable<String> observable = multi.convert()
                .with(MultiRxConverters.toObservable());
        Flowable<String> flowable = multi.convert()
                .with(MultiRxConverters.toFlowable());
        // end::multi-export[]

        completable.test().assertComplete();
        single.test().assertValue(o -> o.isPresent() && o.get().equals("hello"));
        single2.test().assertValue("hello");
        maybe.test().assertValue("hello");
        observable.test().assertValues("hello", "bonjour").assertComplete();
        flowable.test().assertValues("hello", "bonjour").assertComplete();
    }

    private Observable<T> getObservable() {
        return Observable.just("a", "b", "c")
                .map(s -> (T) s);
    }

    private Flowable<T> getFlowable() {
        return Flowable.just("a", "b", "c")
                .map(s -> (T) s);
    }

    private Single<T> getSingle() {
        return Single.just("a")
                .map(s -> (T) s);
    }

    private Maybe<T> getMaybe() {
        return Maybe.just("a")
                .map(s -> (T) s);
    }

    private Maybe<T> getEmptyMaybe() {
        return Maybe.empty();
    }

    private Completable getCompletable() {
        return Completable.complete();
    }

    private Multi<T> getMulti() {
        return Multi.createFrom().items("a", "b", "c")
                .map(s -> (T) s);
    }

    private Uni<T> getUni() {
        return Uni.createFrom().item("a")
                .map(s -> (T) s);
    }
}
