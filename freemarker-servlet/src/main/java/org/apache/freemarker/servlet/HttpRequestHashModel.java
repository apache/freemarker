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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.*;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.model.impl.SimpleString;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * TemplateHashModel wrapper for a HttpServletRequest attributes.
 */
public final class HttpRequestHashModel implements TemplateHashModelEx {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final ObjectWrapperAndUnwrapper wrapper;

    /**
     * @param wrapper
     *            Should be an {@link ObjectWrapperAndUnwrapper}, or else some features might won't work properly. (It's
     *            declared as {@link ObjectWrapper} only for backward compatibility.)
     */
    public HttpRequestHashModel(HttpServletRequest request, ObjectWrapperAndUnwrapper wrapper) {
        this(request, null, wrapper);
    }

    public HttpRequestHashModel(HttpServletRequest request, HttpServletResponse response, ObjectWrapperAndUnwrapper wrapper) {
        this.request = request;
        this.response = response;
        this.wrapper = wrapper;
    }
    
    @Override
    public TemplateModel get(String key) throws TemplateException {
        return wrapper.wrap(request.getAttribute(key));
    }

    @Override
    public boolean isEmptyHash() {
        return !request.getAttributeNames().hasMoreElements();
    }
    
    @Override
    public int getHashSize() {
        int result = 0;
        for (Enumeration enumeration = request.getAttributeNames(); enumeration.hasMoreElements(); ) {
            enumeration.nextElement();
            ++result;
        }
        return result;
    }
    
    @Override
    public TemplateCollectionModel keys() {
        ArrayList keys = new ArrayList();
        for (Enumeration enumeration = request.getAttributeNames(); enumeration.hasMoreElements(); ) {
            keys.add(enumeration.nextElement());
        }
        return new SimpleCollection(keys, wrapper);
    }
    
    @Override
    public TemplateCollectionModel values() {
        ArrayList values = new ArrayList();
        for (Enumeration enumeration = request.getAttributeNames(); enumeration.hasMoreElements(); ) {
            values.add(request.getAttribute((String) enumeration.nextElement()));
        }
        return new SimpleCollection(values, wrapper);
    }
    
    @Override
    public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
        final Enumeration<String> namesEnum = request.getAttributeNames();
        return new KeyValuePairIterator() {
            @Override
            public boolean hasNext() throws TemplateException {
                return namesEnum.hasMoreElements();
            }

            @Override
            public KeyValuePair next() throws TemplateException {
                final String name = namesEnum.nextElement();
                return new KeyValuePair() {
                    @Override
                    public TemplateModel getValue() throws TemplateException {
                        return wrapper.wrap(request.getAttribute(name));
                    }
                    
                    @Override
                    public TemplateModel getKey() throws TemplateException {
                        return new SimpleString(name);
                    }
                };
            }
        };
    }

    public HttpServletRequest getRequest() {
        return request;
    }
    
    public HttpServletResponse getResponse() {
        return response;
    }
    
    public ObjectWrapperAndUnwrapper getObjectWrapper() {
        return wrapper;
    }
}
