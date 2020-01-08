package io.smallrye.mutiny.context;

public class MyContext {
    private static ThreadLocal<MyContext> context = new ThreadLocal<>();

    public static void init() {
        context.set(new MyContext());
    }

    public static void clear() {
        context.remove();
    }

    public static MyContext get() {
        return context.get();
    }

    public static void set(MyContext newContext) {
        context.set(newContext);
    }

    private String reqId;

    public void set(String reqId) {
        this.reqId = reqId;
    }

    public String getReqId() {
        return reqId;
    }
}
