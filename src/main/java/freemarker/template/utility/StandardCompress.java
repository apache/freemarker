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
import java.util.Map;

import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateTransformModel;

/**
 * <p>A filter that compresses each sequence of consecutive whitespace
 * to a single line break (if the sequence contains a line break) or a
 * single space. In addition, leading and trailing whitespace is
 * completely removed.</p>
 * 
 * <p>Specify the transform parameter <code>single_line = true</code>
 * to always compress to a single space instead of a line break.</p>
 * 
 * <p>The default buffer size can be overridden by specifying a
 * <code>buffer_size</code> transform parameter (in bytes).</p>
 *
 * <p><b>Note:</b> The compress tag is implemented using this filter</p>
 * 
 * <p>Usage:<br>
 * From java:</p>
 * <pre>
 * SimpleHash root = new SimpleHash();
 *
 * root.put( "standardCompress", new freemarker.template.utility.StandardCompress() );
 *
 * ...
 * </pre>
 *
 * <p>From your FreeMarker template:</p>
 * <pre>
 * &lt;transform standardCompress&gt;
 *   &lt;p&gt;This    paragraph will have
 *       extraneous
 *
 * whitespace removed.&lt;/p&gt;
 * &lt;/transform&gt;
 * </pre>
 *
 * <p>Output:</p>
 * <pre>
 * &lt;p&gt;This paragraph will have
 * extraneous
 * whitespace removed.&lt;/p&gt;
 * </pre>
 */
public class StandardCompress implements TemplateTransformModel {
    private static final String BUFFER_SIZE_KEY = "buffer_size";
    private static final String SINGLE_LINE_KEY = "single_line";
    private int defaultBufferSize;

    public static final StandardCompress INSTANCE = new StandardCompress();
    
    public StandardCompress()
    {
        this(2048);
    }

    /**
     * @param defaultBufferSize the default amount of characters to buffer
     */
    public StandardCompress(int defaultBufferSize)
    {
        this.defaultBufferSize = defaultBufferSize;
    }

    public Writer getWriter(final Writer out, Map args)
    throws TemplateModelException
    {
        int bufferSize = defaultBufferSize;
        boolean singleLine = false;
        if (args != null) {
            try {
                TemplateNumberModel num = (TemplateNumberModel)args.get(BUFFER_SIZE_KEY);
                if (num != null)
                    bufferSize = num.getAsNumber().intValue();
            } catch (ClassCastException e) {
                throw new TemplateModelException("Expecting numerical argument to " + BUFFER_SIZE_KEY);
            }
            try {
                TemplateBooleanModel flag = (TemplateBooleanModel)args.get(SINGLE_LINE_KEY);
                if (flag != null)
                    singleLine = flag.getAsBoolean();
            } catch (ClassCastException e) {
                throw new TemplateModelException("Expecting boolean argument to " + SINGLE_LINE_KEY);
            }
        }
        return new StandardCompressWriter(out, bufferSize, singleLine);
    }

    private static class StandardCompressWriter extends Writer
    {
        private static final int MAX_EOL_LENGTH = 2; // CRLF is two bytes
        
        private static final int AT_BEGINNING = 0;
        private static final int SINGLE_LINE = 1;
        private static final int INIT = 2;
        private static final int SAW_CR = 3;
        private static final int LINEBREAK_CR = 4;
        private static final int LINEBREAK_CRLF = 5;
        private static final int LINEBREAK_LF = 6;

        private final Writer out;
        private final char[] buf;
        private final boolean singleLine;
    
        private int pos = 0;
        private boolean inWhitespace = true;
        private int lineBreakState = AT_BEGINNING;

        public StandardCompressWriter(Writer out, int bufSize, boolean singleLine) {
            this.out = out;
            this.singleLine = singleLine;
            buf = new char[bufSize];
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            for (;;) {
                // Need to reserve space for the EOL potentially left in the state machine
                int room = buf.length - pos - MAX_EOL_LENGTH; 
                if (room >= len) {
                    writeHelper(cbuf, off, len);
                    break;
                } else if (room <= 0) {
                    flushInternal();
                } else {
                    writeHelper(cbuf, off, room);
                    flushInternal();
                    off += room;
                    len -= room;
                }
            }
        }

        private void writeHelper(char[] cbuf, int off, int len) {
            for (int i = off, end = off + len; i < end; i++) {
                char c = cbuf[i];
                if (Character.isWhitespace(c)) {
                    inWhitespace = true;
                    updateLineBreakState(c);
                } else if (inWhitespace) {
                    inWhitespace = false;
                    writeLineBreakOrSpace();
                    buf[pos++] = c;
                } else {
                    buf[pos++] = c;
                }
            }
        }

        /*
          \r\n    => CRLF
          \r[^\n] => CR
          \r$     => CR
          [^\r]\n => LF
          ^\n     => LF
        */
        private void updateLineBreakState(char c)
        {
            switch (lineBreakState) {
            case INIT:
                if (c == '\r') {
                    lineBreakState = SAW_CR;
                } else if (c == '\n') {
                    lineBreakState = LINEBREAK_LF;
                }
                break;
            case SAW_CR:
                if (c == '\n') {
                    lineBreakState = LINEBREAK_CRLF;
                } else {
                    lineBreakState = LINEBREAK_CR;
                }
            }
        }

        private void writeLineBreakOrSpace()
        {
            switch (lineBreakState) {
            case SAW_CR:
                // whitespace ended with CR, fall through
            case LINEBREAK_CR:
                buf[pos++] = '\r';
                break;
            case LINEBREAK_CRLF:
                buf[pos++] = '\r';
                // fall through
            case LINEBREAK_LF:
                buf[pos++] = '\n';
                break;
            case AT_BEGINNING:
                // ignore leading whitespace
                break;
            case INIT:
            case SINGLE_LINE:
                buf[pos++] = ' ';
            }
            lineBreakState = (singleLine) ? SINGLE_LINE : INIT;
        }

        private void flushInternal() throws IOException {
            out.write(buf, 0, pos);
            pos = 0;
        }

        public void flush() throws IOException {
            flushInternal();
            out.flush();
        }

        public void close() throws IOException {
            flushInternal();
        }
    }
}
