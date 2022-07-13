package guides.integration;

import io.reactivex.rxjava3.core.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.converters.multi.MultiRx3Converters;
import io.smallrye.mutiny.converters.uni.UniRx3Converters;
import mutiny.zero.flow.adapters.AdaptersToFlow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
public class RxJavaTest<T> {

    @Test
    public void testMultiCreation() {
        Observable<T> observable = getObservable();
        Flowable<T> flowable = getFlowable();

        // <rx-multi-create-observable>
        Multi<T> multiFromObservable = Multi.createFrom()
                .converter(MultiRx3Converters.fromObservable(), observable);
        // </rx-multi-create-observable>

        // <rx-multi-create-flowable>
        Flow.Publisher<T> flowableAsPublisher = AdaptersToFlow.publisher(flowable);

        Multi<T> multiFromFlowable = Multi.createFrom().publisher(flowableAsPublisher);
        // </rx-multi-create-flowable>

        List<String> list = multiFromObservable
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a", "b", "c");

        list = multiFromFlowable
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a", "b", "c");

        Completable completable = getCompletable();
        Single<T> single = getSingle();
        Maybe<T> maybe = getMaybe();
        Maybe<T> emptyMaybe = getEmptyMaybe();

        // <rx-multi-create-single>
        Multi<Void> multiFromCompletable = Multi.createFrom()
                .converter(MultiRx3Converters.fromCompletable(), completable);
        Multi<T> multiFromSingle = Multi.createFrom()
                .converter(MultiRx3Converters.fromSingle(), single);
        Multi<T> multiFromMaybe = Multi.createFrom()
                .converter(MultiRx3Converters.fromMaybe(), maybe);
        Multi<T> multiFromEmptyMaybe = Multi.createFrom()
                .converter(MultiRx3Converters.fromMaybe(), emptyMaybe);
        // </rx-multi-create-single>

        list = multiFromCompletable
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();

        list = multiFromSingle
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a");

        list = multiFromMaybe
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).containsExactly("a");

        list = multiFromEmptyMaybe
                .onItem().transform(Object::toString)
                .collect().asList().await().indefinitely();
        assertThat(list).isEmpty();
    }

    @Test
    public void testUniCreation() {
        Observable<T> observable = getObservable();
        Flowable<T> flowable = getFlowable();

        // <rx-uni-create-observable>
        Uni<T> uniFromObservable = Uni.createFrom().converter(
                UniRx3Converters.fromObservable(), observable);
        // </rx-uni-create-observable>

        // <rx-uni-create-flowable>
        Flow.Publisher<T> flowableAsPublisher = AdaptersToFlow.publisher(flowable);

        Uni<T> uniFromFlowable = Uni.createFrom().publisher(flowableAsPublisher);
        // </rx-uni-create-flowable>

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

        // <rx-uni-create-single>
        Uni<Void> multiFromCompletable = Uni.createFrom()
                .converter(UniRx3Converters.fromCompletable(), completable);
        Uni<T> multiFromSingle = Uni.createFrom()
                .converter(UniRx3Converters.fromSingle(), single);
        Uni<T> multiFromMaybe = Uni.createFrom()
                .converter(UniRx3Converters.fromMaybe(), maybe);
        Uni<T> multiFromEmptyMaybe = Uni.createFrom()
                .converter(UniRx3Converters.fromMaybe(), emptyMaybe);
        // </rx-uni-create-single>

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

        // <create-rx-from-multi>
        Completable completable = multi.convert()
                .with(MultiRx3Converters.toCompletable());
        Single<Optional<T>> single = multi.convert()
                .with(MultiRx3Converters.toSingle());
        Single<T> single2 = multi.convert()
                .with(MultiRx3Converters
                        .toSingle().onEmptyThrow(() -> new Exception("D'oh!")));
        Maybe<T> maybe = multi.convert()
                .with(MultiRx3Converters.toMaybe());
        Observable<T> observable = multi.convert()
                .with(MultiRx3Converters.toObservable());
        Flowable<T> flowable = multi.convert()
                .with(MultiRx3Converters.toFlowable());
        // </create-rx-from-multi>

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

        // <create-rx-from-uni>
        Completable completable = uni.convert().with(UniRx3Converters.toCompletable());
        Single<Optional<T>> single = uni.convert().with(UniRx3Converters.toSingle());
        Single<T> single2 = uni.convert().with(UniRx3Converters.toSingle().failOnNull());
        Maybe<T> maybe = uni.convert().with(UniRx3Converters.toMaybe());
        Observable<T> observable = uni.convert().with(UniRx3Converters.toObservable());
        Flowable<T> flowable = uni.convert().with(UniRx3Converters.toFlowable());
        // </create-rx-from-uni>

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
        // <uni-export>
        Completable completable = uni.convert().with(UniRx3Converters.toCompletable());
        Single<Optional<String>> single = uni.convert().with(UniRx3Converters.toSingle());
        Single<String> single2 = uni.convert().with(UniRx3Converters.toSingle().failOnNull());
        Maybe<String> maybe = uni.convert().with(UniRx3Converters.toMaybe());
        Observable<String> observable = uni.convert().with(UniRx3Converters.toObservable());
        Flowable<String> flowable = uni.convert().with(UniRx3Converters.toFlowable());
        // </uni-export>

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
        // <multi-export>
        Completable completable = multi.convert()
                .with(MultiRx3Converters.toCompletable());
        Single<Optional<String>> single = multi.convert()
                .with(MultiRx3Converters.toSingle());
        Single<String> single2 = multi.convert()
                .with(MultiRx3Converters.toSingle().onEmptyThrow(() -> new Exception("D'oh!")));
        Maybe<String> maybe = multi.convert()
                .with(MultiRx3Converters.toMaybe());
        Observable<String> observable = multi.convert()
                .with(MultiRx3Converters.toObservable());
        Flowable<String> flowable = multi.convert()
                .with(MultiRx3Converters.toFlowable());
        // </multi-export>

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
