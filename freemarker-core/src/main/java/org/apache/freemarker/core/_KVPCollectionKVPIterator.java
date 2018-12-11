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

import java.util.Collection;
import java.util.Iterator;

import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePairIterator;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 */
public class _KVPCollectionKVPIterator implements KeyValuePairIterator {
    private final Iterator<KeyValuePair> iter;
    
    public _KVPCollectionKVPIterator(Collection<KeyValuePair> kvps) {
        iter = kvps.iterator();
    }


    @Override
    public boolean hasNext() throws TemplateException {
        return iter.hasNext();
    }

    @Override
    public KeyValuePair next() throws TemplateException {
        return iter.next();
    }
}