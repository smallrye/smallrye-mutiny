package guides.extension;

import org.junit.jupiter.api.extension.*;

public class SystemOutCaptureExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback,
        ParameterResolver {

    private SystemOut out;

    @Override
    public void afterTestExecution(ExtensionContext context) {
        if (out != null) {
            out.reset();
            out.close();
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        out = new SystemOut(System.out);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == SystemOut.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        if (parameterType == SystemOut.class) {
            return out.capture();
        }
        throw new ParameterResolutionException(String.format("Could not resolve parameter of type %s.", parameterType));
    }
}
