/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import freemarker.ext.beans.BeansWrapper;

/**
 * <p>A simple implementation of the <tt>TemplateHashModelEx</tt>
 * interface, using an underlying {@link Map} or {@link SortedMap}.</p>
 *
 * <p>This class is thread-safe if you don't call the <tt>put</tt> or <tt>remove</tt> methods
 * after you have made the object available for multiple threads.
 *
 * <p><b>Note:</b><br />
 * As of 2.0, this class is unsynchronized by default.
 * To obtain a synchronized wrapper, call the {@link #synchronizedWrapper} method.</p>
 *
 * @see SimpleSequence
 * @see SimpleScalar
 */
public class SimpleHash extends WrappingTemplateModel 
implements TemplateHashModelEx, Serializable {

    private Map map;
    private boolean putFailed;
    private Map unwrappedMap;

    /**
     * Constructs an empty hash that uses the default wrapper set in
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}.
     */
    public SimpleHash() {
        this((ObjectWrapper)null);
    }

    /**
     * Creates a new simple hash with the copy of the underlying map and the
     * default wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}.
     * @param map The Map to use for the key/value pairs. It makes a copy for 
     * internal use. If the map implements the {@link SortedMap} interface, the
     * internal copy will be a {@link TreeMap}, otherwise it will be a 
     * {@link HashMap}.
     */
    public SimpleHash(Map map) {
        this(map, null);
    }

    /**
     * Creates an empty simple hash using the specified object wrapper.
     * @param wrapper The object wrapper to use to wrap objects into
     * {@link TemplateModel} instances. If null, the default wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)} is
     * used.
     */
    public SimpleHash(ObjectWrapper wrapper) {
        super(wrapper);
        map = new HashMap();
    }

    /**
     * Creates a new simple hash with the copy of the underlying map and 
     * either the default wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}, or
     * the {@link freemarker.ext.beans.BeansWrapper JavaBeans wrapper}.
     * @param map The Map to use for the key/value pairs. It makes a copy for 
     * internal use. If the map implements the {@link SortedMap} interface, the
     * internal copy will be a {@link TreeMap}, otherwise it will be a 
     * @param wrapper The object wrapper to use to wrap objects into
     * {@link TemplateModel} instances. If null, the default wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)} is
     * used.
     */
    public SimpleHash(Map map, ObjectWrapper wrapper) {
        super(wrapper);
        try {
            this.map = copyMap(map);
        } catch (ConcurrentModificationException cme) {
            //This will occur extremely rarely.
            //If it does, we just wait 5 ms and try again. If 
            // the ConcurrentModificationException
            // is thrown again, we just let it bubble up this time.
            // TODO: Maybe we should log here.
            try {
                Thread.sleep(5);
            } catch (InterruptedException ie) {
            }
            synchronized (map) {
                this.map = copyMap(map);
            }
        }
    }

    protected Map copyMap(Map map) {
        if (map instanceof HashMap) {
            return (Map) ((HashMap) map).clone();
        }
        if (map instanceof SortedMap) {
            if (map instanceof TreeMap) {
                return (Map) ((TreeMap) map).clone();
            }
            else {
                return new TreeMap((SortedMap) map);
            }
        } 
        return new HashMap(map);
    }

    /**
     * Adds a key-value entry to the map.
     *
     * @param key the name by which the object is
     * identified in the template.
     * @param obj the object to store.
     */
    public void put(String key, Object obj) {
        map.put(key, obj);
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

    public TemplateModel get(String key) throws TemplateModelException {
        Object result = map.get(key);
        // The key to use for putting -- it's the key that already exists in
        // the map (either key or charKey below). This way, we'll never put a 
        // new key in the map, avoiding spurious ConcurrentModificationException
        // from another thread iterating over the map, see bug #1939742 in 
        // SourceForge tracker.
        final Object putKey;
        if (result == null) {
            if(key.length() == 1) {
                // just check for Character key if this is a single-character string
                Character charKey = new Character(key.charAt(0));
                result = map.get(charKey);
                if (result == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                    return null;
                }
                else {
                    putKey = charKey;
                }
            }
            else if(!map.containsKey(key)) {
                return null;
            }
            else {
                putKey = key;
            }
        }
        else {
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
        for (Iterator it = m.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            this.put((String) entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Note that this method creates and returns a deep-copy of the underlying hash used
     * internally. This could be a gotcha for some people
     * at some point who want to alter something in the data model,
     * but we should maintain our immutability semantics (at least using default SimpleXXX wrappers) 
     * for the data model. It will recursively unwrap the stuff in the underlying container. 
     */
    public Map toMap() throws TemplateModelException {
        if (unwrappedMap == null) {
            Class mapClass = this.map.getClass();
            Map m = null;
            try {
                m = (Map) mapClass.newInstance();
            } catch (Exception e) {
                throw new TemplateModelException("Error instantiating map of type " + mapClass.getName() + "\n" + e.getMessage());
            }
            // Create a copy to maintain immutability semantics and
            // Do nested unwrapping of elements if necessary.
            BeansWrapper bw = BeansWrapper.getDefaultInstance();
            for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof TemplateModel) {
                    value = bw.unwrap((TemplateModel) value);
                }
                m.put(key, value);
            }
            unwrappedMap=m;
        }
        return unwrappedMap;
    }

    /**
     * Convenience method for returning the <tt>String</tt> value of the
     * underlying map.
     */
    public String toString() {
        return map.toString();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map == null || map.isEmpty();
    }

    public TemplateCollectionModel keys() {
        return new SimpleCollection(map.keySet(), getObjectWrapper());
    }

    public TemplateCollectionModel values() {
        return new SimpleCollection(map.values(), getObjectWrapper());
    }

    public SimpleHash synchronizedWrapper() {
        return new SynchronizedHash();
    }
    
    
    private class SynchronizedHash extends SimpleHash {

        public boolean isEmpty() {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.isEmpty();
            }
        }
        
        public void put(String key, Object obj) {
            synchronized (SimpleHash.this) {
                SimpleHash.this.put(key, obj);
            }
        }

        public TemplateModel get(String key) throws TemplateModelException {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.get(key);
            }
        }

        public void remove(String key) {
            synchronized (SimpleHash.this) {
                SimpleHash.this.remove(key);
            }
        }

        public int size() {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.size();
            }
        }

        public TemplateCollectionModel keys() {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.keys();
            }
        }

        public TemplateCollectionModel values() {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.values();
            }
        }
        
        public Map toMap() throws TemplateModelException {
            synchronized (SimpleHash.this) {
                return SimpleHash.this.toMap();
            }
        }
    
    }
}
