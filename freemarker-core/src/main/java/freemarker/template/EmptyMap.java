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

package freemarker.template;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Read-only empty map. {@link #remove(Object)}, {@link #clear()} and
 * {@link #putAll(Map)} with an empty {@link Map} as parameter are supported
 * operations (and do nothing) since FreeMarker 2.3.20. 
 * 
 * @deprecated Use {@link Collections#EMPTY_MAP} on J2SE 1.3 or later.   
 */
@Deprecated
public class EmptyMap implements Map, Cloneable {
    public static final EmptyMap instance = new EmptyMap(); 
    
    @Override
    public void clear() {
        // no op
    }
    
    @Override
    public boolean containsKey(Object arg0) {
        return false;
    }
    
    @Override
    public boolean containsValue(Object arg0) {
        return false;
    }
    
    @Override
    public Set entrySet() {
        return Collections.EMPTY_SET;
    }
    
    @Override
    public Object get(Object arg0) {
        return null;
    }
    
    @Override
    public boolean isEmpty() {
        return true;
    }
    
    @Override
    public Set keySet() {
        return Collections.EMPTY_SET;
    }
    
    @Override
    public Object put(Object arg0, Object arg1) {
        throw new UnsupportedOperationException("This Map is read-only.");
    }
    
    @Override
    public void putAll(Map arg0) {
        // Checking for arg0.isEmpty() wouldn't reflect precisely how putAll in
        // AbstractMap works. 
        if (arg0.entrySet().iterator().hasNext()) {
            throw new UnsupportedOperationException("This Map is read-only.");
        }
    }
    
    @Override
    public Object remove(Object arg0) {
        return null;
    }
    
    @Override
    public int size() {
        return 0;
    }
    
    @Override
    public Collection values() {
        return Collections.EMPTY_LIST;
    }

}
