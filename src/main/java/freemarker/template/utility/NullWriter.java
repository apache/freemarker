package freemarker.template.utility;

import java.io.IOException;
import java.io.Writer;

/**
 * A {@link Writer} that simply drops what it gets.
 * 
 * @since 2.3.20
 */
public final class NullWriter extends Writer {
    
    public static final NullWriter INSTANCE = new NullWriter();
    
    /** Can't be instantiated; use {@link #INSTANCE}. */
    private NullWriter() { }
    
    public void write(char[] cbuf, int off, int len) throws IOException {
        // Do nothing
    }

    public void flush() throws IOException {
        // Do nothing
    }

    public void close() throws IOException {
        // Do nothing
    }

    public void write(int c) throws IOException {
        // Do nothing
    }

    public void write(char[] cbuf) throws IOException {
        // Do nothing
    }

    public void write(String str) throws IOException {
        // Do nothing
    }

    public void write(String str, int off, int len) throws IOException {
        // Do nothing
    }

    // Put these back in Java 5:
    /*
    public Writer append(CharSequence csq) throws IOException {
        // Do nothing
        return this;
    }

    public Writer append(CharSequence csq, int start, int end) throws IOException {
        // Do nothing
        return this;
    }

    public Writer append(char c) throws IOException {
        // Do nothing
        return this;
    }
    */

}
