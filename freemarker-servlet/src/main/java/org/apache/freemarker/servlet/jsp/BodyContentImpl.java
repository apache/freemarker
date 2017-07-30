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

package org.apache.freemarker.servlet.jsp;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

/**
 * An implementation of BodyContent that buffers it's input to a char[].
 */
class BodyContentImpl extends BodyContent {
    private CharArrayWriter buf;

    BodyContentImpl(JspWriter out, boolean buffer) {
        super(out);
        if (buffer) initBuffer();
    }

    void initBuffer() {
        buf = new CharArrayWriter();
    }

    @Override
    public void flush() throws IOException {
        if (buf == null) {
            getEnclosingWriter().flush();
        }
    }

    @Override
    public void clear() throws IOException {
        if (buf != null) {
            buf = new CharArrayWriter();
        } else {
            throw new IOException("Can't clear");
        }
    }

    @Override
    public void clearBuffer() throws IOException {
        if (buf != null) {
            buf = new CharArrayWriter();
        } else {
            throw new IOException("Can't clear");
        }
    }

    @Override
    public int getRemaining() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void newLine() throws IOException {
        write(JspWriterAdapter.NEWLINE);
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void print(boolean arg0) throws IOException {
        write(arg0 ? Boolean.TRUE.toString() : Boolean.FALSE.toString());
    }

    @Override
    public void print(char arg0) throws IOException {
        write(arg0);
    }

    @Override
    public void print(char[] arg0) throws IOException {
        write(arg0);
    }

    @Override
    public void print(double arg0) throws IOException {
        write(Double.toString(arg0));
    }

    @Override
    public void print(float arg0) throws IOException {
        write(Float.toString(arg0));
    }

    @Override
    public void print(int arg0) throws IOException {
        write(Integer.toString(arg0));
    }

    @Override
    public void print(long arg0) throws IOException {
        write(Long.toString(arg0));
    }

    @Override
    public void print(Object arg0) throws IOException {
        write(arg0 == null ? "null" : arg0.toString());
    }

    @Override
    public void print(String arg0) throws IOException {
        write(arg0);
    }

    @Override
    public void println() throws IOException {
        newLine();
    }

    @Override
    public void println(boolean arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(char arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(char[] arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(double arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(float arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(int arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(long arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(Object arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void println(String arg0) throws IOException {
        print(arg0);
        newLine();
    }

    @Override
    public void write(int c) throws IOException {
        if (buf != null) {
            buf.write(c);
        } else {
            getEnclosingWriter().write(c);
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (buf != null) {
            buf.write(cbuf, off, len);
        } else {
            getEnclosingWriter().write(cbuf, off, len);
        }
    }

    @Override
    public String getString() {
        return buf.toString();
    }

    @Override
    public Reader getReader() {
        return new CharArrayReader(buf.toCharArray());
    }

    @Override
    public void writeOut(Writer out) throws IOException {
        buf.writeTo(out);
    }

}
