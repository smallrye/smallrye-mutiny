package io.smallrye.mutiny.context;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

public class MyThreadContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        MyContext capturedContext = MyContext.get();
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.set(capturedContext);
            return () -> {
                MyContext.set(movedContext);
            };
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            MyContext movedContext = MyContext.get();
            MyContext.clear();
            return () -> {
                if (movedContext == null)
                    MyContext.clear();
                else
                    MyContext.set(movedContext);
            };
        };
    }

    @Override
    public String getThreadContextType() {
        return "MyContext";
    }

}
