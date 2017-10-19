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
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.impl.SimpleCollection;
import org.apache.freemarker.core.model.impl.SimpleString;

/**
 * TemplateHashModel wrapper for a HttpServletRequest parameters.
 */

public class HttpRequestParametersHashModel implements TemplateHashModelEx {
    private final HttpServletRequest request;
    private final ObjectWrapper objectWrapper;
    private List<String> keys;
        
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
    public TemplateCollectionModel keys() {
        return new SimpleCollection(getKeys(), objectWrapper);
    }
    
    @Override
    public TemplateCollectionModel values() {
        return new TemplateCollectionModel() {
            private final List<String> paramNames = getKeys();

            @Override
            public int getCollectionSize() throws TemplateException {
                return paramNames.size();
            }

            @Override
            public boolean isEmptyCollection() throws TemplateException {
                return paramNames.isEmpty();
            }

            @Override
            public TemplateModelIterator iterator() throws TemplateException {
                return new TemplateModelIterator() {
                    int nextIndex;

                    @Override
                    public TemplateModel next() throws TemplateException {
                        TemplateModel result = objectWrapper.wrap(
                                request.getParameter(paramNames.get(nextIndex)));
                        nextIndex++;
                        return result;
                    }

                    @Override
                    public boolean hasNext() throws TemplateException {
                        return nextIndex < paramNames.size();
                    }
                };
            }
        };
    }
    
    

    @Override
    public KeyValuePairIterator keyValuePairIterator() throws TemplateException {
        return new KeyValuePairIterator() {
            private final List<String> keys = getKeys();
            private int nextIndex = 0;  
            
            @Override
            public KeyValuePair next() throws TemplateException {
                return new KeyValuePair() {
                    private final String key = keys.get(nextIndex++);
                    
                    @Override
                    public TemplateModel getValue() throws TemplateException {
                        return objectWrapper.wrap(request.getParameter(key));
                    }
                    
                    @Override
                    public TemplateModel getKey() throws TemplateException {
                        return new SimpleString(key);
                    }
                };
            }
            
            @Override
            public boolean hasNext() throws TemplateException {
                return nextIndex < keys.size();
            }
        };
    }

    private synchronized List<String> getKeys() {
        if (keys == null) {
            keys = new ArrayList<>();
            for (Enumeration<String> enumeration = request.getParameterNames(); enumeration.hasMoreElements(); ) {
                keys.add(enumeration.nextElement());
            }
        }
        return keys;
    }
}
