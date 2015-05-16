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
package freemarker.template;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;

import org.junit.Assert;
import org.junit.Test;

/**
 * JavaCC suppresses exceptions thrown by the Reader, silently treating them as EOF. To be precise, JavaCC 3.2 only does
 * that with {@link IOException}-s, while JavaCC 6 does that for all {@link Exception}-s. This tests FreeMarker's
 * workaround for this problem.
 */
public class JavaCCExceptionAsEOFFixTest {

    public static class FailingReader extends Reader {

        private static final String CONTENT = "abc";

        private final Throwable exceptionToThrow;
        private int readSoFar;

        protected FailingReader(Throwable exceptionToThrow) {
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public int read() throws IOException {
            if (readSoFar == CONTENT.length()) {
                if (exceptionToThrow != null) {
                    throwException();
                } else {
                    return -1;
                }
            }
            return CONTENT.charAt(readSoFar++);
        }

        private void throwException() throws IOException {
            if (exceptionToThrow instanceof IOException) {
                throw (IOException) exceptionToThrow;
            }
            if (exceptionToThrow instanceof RuntimeException) {
                throw (RuntimeException) exceptionToThrow;
            }
            if (exceptionToThrow instanceof Error) {
                throw (Error) exceptionToThrow;
            }
            Assert.fail();
        }

        @Override
        public void close() throws IOException {
            // nop
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            for (int i = 0; i < len; i++) {
                int c = read();
                if (c == -1) return i == 0 ? -1 : i;
                cbuf[off + i] = (char) c;
            }
            return len;
        }

    }

    @Test
    public void testIOException() throws IOException {
        try {
            new Template(null, new FailingReader(new IOException("test")), new Configuration());
            fail();
        } catch (IOException e) {
            assertEquals("test", e.getMessage());
        }
    }

    @Test
    public void testRuntimeException() throws IOException {
        try {
            new Template(null, new FailingReader(new NullPointerException("test")), new Configuration());
            fail();
        } catch (NullPointerException e) {
            assertEquals("test", e.getMessage());
        }
    }

    @Test
    public void testError() throws IOException {
        try {
            new Template(null, new FailingReader(new OutOfMemoryError("test")), new Configuration());
            fail();
        } catch (OutOfMemoryError e) {
            assertEquals("test", e.getMessage());
        }
    }

    @Test
    public void testNoException() throws IOException {
        Template t = new Template(null, new FailingReader(null), new Configuration());
        assertEquals("abc", t.toString());
    }

}
