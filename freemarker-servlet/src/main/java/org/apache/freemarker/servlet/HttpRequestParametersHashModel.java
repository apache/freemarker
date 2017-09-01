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

package org.apache.freemarker.servlet;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.impl.SimpleIterable;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * TemplateHashModel wrapper for a HttpServletRequest parameters.
 */

public class HttpRequestParametersHashModel implements TemplateHashModelEx {
    private final HttpServletRequest request;
    private final ObjectWrapper objectWrapper;
    private List keys;
        
    public HttpRequestParametersHashModel(HttpServletRequest request, ObjectWrapper objectWrapper) {
        this.request = request;
        this.objectWrapper = objectWrapper;
    }

    @Override
    public TemplateModel get(String key) {
        String value = request.getParameter(key);
        return value == null ? null : new SimpleString(value);
    }

    @Override
    public boolean isEmptyHash() {
        return !request.getParameterNames().hasMoreElements();
    }
    
    @Override
    public int getHashSize() {
        return getKeys().size();
    }
    
    @Override
    public TemplateIterableModel keys() {
        return new SimpleIterable(getKeys().iterator(), objectWrapper);
    }
    
    @Override
    public TemplateIterableModel values() {
        final Iterator iter = getKeys().iterator();
        return new SimpleIterable(
            new Iterator() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }
                @Override
                public Object next() {
                    return request.getParameter((String) iter.next());
                }
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }, objectWrapper);
    }

    protected String transcode(String string) {
        return string;
    }

    private synchronized List getKeys() {
        if (keys == null) {
            keys = new ArrayList();
            for (Enumeration enumeration = request.getParameterNames(); enumeration.hasMoreElements(); ) {
                keys.add(enumeration.nextElement());
            }
        }
        return keys;
    }
}
