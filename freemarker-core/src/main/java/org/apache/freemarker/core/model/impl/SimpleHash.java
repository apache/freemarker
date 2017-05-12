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

package org.apache.freemarker.core.model.impl;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateCollectionModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.model.WrappingTemplateModel;

/**
 * A simple implementation of the {@link TemplateHashModelEx} interface, using its own underlying {@link Map} or
 * {@link SortedMap} for storing the hash entries. If you are wrapping an already existing {@link Map}, you should
 * certainly use {@link DefaultMapAdapter} instead (see comparison below).
 *
 * <p>
 * This class is thread-safe if you don't call modifying methods (like {@link #put(String, Object)},
 * {@link #remove(String)}, etc.) after you have made the object available for multiple threads (assuming you have
 * published it safely to the other threads; see JSR-133 Java Memory Model). These methods aren't called by FreeMarker,
 * so it's usually not a concern.
 * 
 * <p>
 * <b>{@link SimpleHash} VS {@link DefaultMapAdapter} - Which to use when?</b>
 * 
 * <p>
 * For a {@link Map} that exists regardless of FreeMarker, only you need to access it from templates,
 * {@link DefaultMapAdapter} should be the default choice, as it reflects the exact behavior of the underlying
 * {@link Map} (no surprises), can be unwrapped to the originally wrapped object (important when passing it to Java
 * methods from the template), and has more predictable performance (no spikes).
 * 
 * <p>
 * For a hash that's made specifically to be used from templates, creating an empty {@link SimpleHash} then filling it
 * with {@link SimpleHash#put(String, Object)} is usually the way to go, as the resulting hash is significantly faster
 * to read from templates than a {@link DefaultMapAdapter} (though it's somewhat slower to read from a plain Java method
 * to which it had to be passed adapted to a {@link Map}).
 * 
 * <p>
 * It also matters if for how many times will the <em>same</em> {@link Map} entry be read from the template(s) later, on
 * average. If, on average, you read each entry for more than 4 times, {@link SimpleHash} will be most certainly faster,
 * but if for 2 times or less (and especially if not at all) then {@link DefaultMapAdapter} will be faster. Before
 * choosing based on performance though, pay attention to the behavioral differences; {@link SimpleHash} will
 * shallow-copy the original {@link Map} at construction time, so key order will be lost in some cases, and it won't
 * reflect {@link Map} content changes after the {@link SimpleHash} construction, also {@link SimpleHash} can't be
 * unwrapped to the original {@link Map} instance.
 *
 * @see DefaultMapAdapter
 * @see TemplateHashModelEx
 */
public class SimpleHash extends WrappingTemplateModel implements TemplateHashModelEx2, Serializable {

    private final Map map;
    private boolean putFailed;
    private Map unwrappedMap;

    /**
     * Creates an empty simple hash using the specified object wrapper.
     * @param wrapper The object wrapper to use to wrap objects into
     * {@link TemplateModel} instances. Not {@code null}.
     */
    public SimpleHash(ObjectWrapper wrapper) {
        super(wrapper);
        map = new HashMap();
    }

    /**
     * Creates a new hash by shallow-coping (possibly cloning) the underlying map; in many applications you should use
     * {@link DefaultMapAdapter} instead.
     *
     * @param map
     *            The Map to use for the key/value pairs. It makes a copy for internal use. If the map implements the
     *            {@link SortedMap} interface, the internal copy will be a {@link TreeMap}, otherwise it will be a
     * @param wrapper
     *            The object wrapper to use to wrap contained objects into {@link TemplateModel} instances. Not
     *            {@code null}.
     */
    public SimpleHash(Map map, ObjectWrapper wrapper) {
        super(wrapper);
        Map mapCopy;
        try {
            mapCopy = copyMap(map);
        } catch (ConcurrentModificationException cme) {
            //This will occur extremely rarely.
            //If it does, we just wait 5 ms and try again. If 
            // the ConcurrentModificationException
            // is thrown again, we just let it bubble up this time.
            // TODO: Maybe we should log here.
            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
                // Ignored
            }
            synchronized (map) {
                mapCopy = copyMap(map);
            }
        }
        this.map = mapCopy;
    }

    protected Map copyMap(Map map) {
        if (map instanceof HashMap) {
            return (Map) ((HashMap) map).clone();
        }
        if (map instanceof SortedMap) {
            if (map instanceof TreeMap) {
                return (Map) ((TreeMap) map).clone();
            } else {
                return new TreeMap((SortedMap) map);
            }
        } 
        return new HashMap(map);
    }

    /**
     * Adds a key-value entry to this hash.
     *
     * @param key
     *            The name by which the object is identified in the template.
     * @param value
     *            The value to which the name will be associated. This will only be wrapped to {@link TemplateModel}
     *            lazily when it's first read.
     */
    public void put(String key, Object value) {
        map.put(key, value);
        unwrappedMap = null;
    }

    /**
     * Puts a boolean in the map
     *
     * @param key the name by which the resulting <tt>TemplateModel</tt>
     * is identified in the template.
     * @param b the boolean to store.
     */
    public void put(String key, boolean b) {
        put(key, b ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE);
    }

    @Override
    public TemplateModel get(String key) throws TemplateModelException {
        Object result;
        try {
            result = map.get(key);
        } catch (ClassCastException e) {
            throw new _TemplateModelException(e,
                    "ClassCastException while getting Map entry with String key ",
                    new _DelayedJQuote(key));
        } catch (NullPointerException e) {
            throw new _TemplateModelException(e,
                    "NullPointerException while getting Map entry with String key ",
                    new _DelayedJQuote(key));
        }
        // The key to use for putting -- it's the key that already exists in
        // the map (either key or charKey below). This way, we'll never put a 
        // new key in the map, avoiding spurious ConcurrentModificationException
        // from another thread iterating over the map, see bug #1939742 in 
        // SourceForge tracker.
        Object putKey = null;
        if (result == null) {
            // Check for Character key if this is a single-character string.
            // In SortedMap-s, however, we can't do that safely, as it can cause ClassCastException.
            if (key.length() == 1 && !(map instanceof SortedMap)) {
                Character charKey = Character.valueOf(key.charAt(0));
                try {
                    result = map.get(charKey);
                    if (result != null || map.containsKey(charKey)) {
                        putKey = charKey;
                    }
                } catch (ClassCastException e) {
                    throw new _TemplateModelException(e,
                            "ClassCastException while getting Map entry with Character key ",
                            new _DelayedJQuote(key));
                } catch (NullPointerException e) {
                    throw new _TemplateModelException(e,
                            "NullPointerException while getting Map entry with Character key ",
                            new _DelayedJQuote(key));
                }
            }
            if (putKey == null) {
                if (!map.containsKey(key)) {
                    return null;
                } else {
                    putKey = key;
                }
            }
        } else {
            putKey = key;
        }
        
        if (result instanceof TemplateModel) {
            return (TemplateModel) result;
        }
        
        TemplateModel tm = wrap(result);
        if (!putFailed) {
            try {
                map.put(putKey, tm);
            } catch (Exception e) {
                // If it's immutable or something, we just keep going.
                putFailed = true;
            }
        }
        return tm;
    }

    /**
     * Tells if the map contains a key or not, regardless if the associated value is {@code null} or not.
     * @since 2.3.20
     */
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    /**
     * Removes the given key from the underlying map.
     *
     * @param key the key to be removed
     */
    public void remove(String key) {
        map.remove(key);
    }

    /**
     * Adds all the key/value entries in the map
     * @param m the map with the entries to add, the keys are assumed to be strings.
     */

    public void putAll(Map m) {
        for (Iterator it = m.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            put((String) entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the {@code toString()} of the underlying {@link Map}.
     */
    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map == null || map.isEmpty();
    }

    @Override
    public TemplateCollectionModel keys() {
        return new SimpleCollection(map.keySet(), getObjectWrapper());
    }

    @Override
    public TemplateCollectionModel values() {
        return new SimpleCollection(map.values(), getObjectWrapper());
    }

    @Override
    public KeyValuePairIterator keyValuePairIterator() {
        return new MapKeyValuePairIterator(map, getObjectWrapper());
    }
}
