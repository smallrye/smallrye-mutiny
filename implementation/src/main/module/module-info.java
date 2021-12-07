open module io.smallrye.mutiny {

    requires org.reactivestreams;

    uses io.smallrye.mutiny.infrastructure.MultiInterceptor;
    uses io.smallrye.mutiny.infrastructure.ExecutorConfiguration;
    uses io.smallrye.mutiny.infrastructure.UniInterceptor;
    uses io.smallrye.mutiny.infrastructure.CallbackDecorator;
}