/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
