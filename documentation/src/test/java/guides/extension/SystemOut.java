package guides.extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class SystemOut {

    private final PrintStream stream;
    private final ByteArrayOutputStream baos;
    private final PrintStream original;

    public SystemOut(PrintStream original) {
        this.original = original;
        this.baos = new ByteArrayOutputStream();
        this.stream = new PrintStream(baos);
    }

    public String get() {
        return baos.toString();
    }

    public void close() {
        stream.close();
        try {
            baos.close();
        } catch (IOException e) {
            // Ignore it.
        }
    }

    public void reset() {
        System.setOut(original);
    }

    public SystemOut capture() {
        System.setOut(stream);
        return this;
    }
}
