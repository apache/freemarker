/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import java.io.IOException;
import java.io.Writer;

/**
 * AST directive node: {@code #compress}.
 * An instruction that reduces all sequences of whitespace to a single
 * space or newline. In addition, leading and trailing whitespace is removed.
 */
final class ASTDirCompress extends ASTDirective {

    ASTDirCompress(TemplateElements children) { 
        setChildren(children);
    }

    @Override
    ASTElement[] execute(Environment env) throws TemplateException, IOException {
        ASTElement[] childBuffer = getChildBuffer();
        if (childBuffer != null) {
            CompressWriter out = new CompressWriter(env.getOut(), 2048, false);
            Writer prevOut = env.getOut();
            try {
                env.setOut(out);
                env.executeElements(childBuffer);
            } finally {
                out.close();
                env.setOut(prevOut);
            }
        }
        return null;
    }

    @Override
    String dump(boolean canonical) {
        if (canonical) {
            return "<" + getLabelWithoutParameters() + ">" + getChildrenCanonicalForm() + "</" + getLabelWithoutParameters() + ">";
        } else {
            return getLabelWithoutParameters();
        }
    }
    
    @Override
    public String getLabelWithoutParameters() {
        return "#compress";
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    boolean isIgnorable(boolean stripWhitespace) {
        return getChildCount() == 0 && getParameterCount() == 0;
    }

    @Override
    boolean isNestedBlockRepeater() {
        return false;
    }

    // TODO [FM3] Blindly copied from FM2 StandaradCompress; review
    private static class CompressWriter extends Writer {
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

        public CompressWriter(Writer out, int bufSize, boolean singleLine) {
            this.out = out;
            this.singleLine = singleLine;
            buf = new char[bufSize];
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            for (; ; ) {
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
        private void updateLineBreakState(char c) {
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

        private void writeLineBreakOrSpace() {
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

        @Override
        public void flush() throws IOException {
            flushInternal();
            out.flush();
        }

        @Override
        public void close() throws IOException {
            flushInternal();
        }
    }

}
