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

package org.apache.freemarker.core.util;

import java.io.IOException;
import java.io.Writer;

/**
 * A {@link Writer} that simply drops what it gets.
 */
public final class _NullWriter extends Writer {
    
    public static final _NullWriter INSTANCE = new _NullWriter();
    
    /** Can't be instantiated; use {@link #INSTANCE}. */
    private _NullWriter() { }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // Do nothing
    }

    @Override
    public void flush() throws IOException {
        // Do nothing
    }

    @Override
    public void close() throws IOException {
        // Do nothing
    }

    @Override
    public void write(int c) throws IOException {
        // Do nothing
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        // Do nothing
    }

    @Override
    public void write(String str) throws IOException {
        // Do nothing
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        // Do nothing
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        // Do nothing
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        // Do nothing
        return this;
    }

    @Override
    public Writer append(char c) throws IOException {
        // Do nothing
        return this;
    }

}
