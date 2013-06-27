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

package freemarker.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import freemarker.core._ConcurrentMapFactory;
import freemarker.template.utility.UndeclaredThrowableException;

/**
 * Soft cache storage is a cache storage that uses {@link SoftReference} 
 * objects to hold the objects it was passed, therefore allows the garbage
 * collector to purge the cache when it determines that it wants to free up
 * memory.
 * This class is thread-safe to the extent that its underlying map is. The 
 * default implementation uses a concurrent map on Java 5 and above, so it's
 * thread-safe in that case.
 *
 * @see freemarker.template.Configuration#setCacheStorage(CacheStorage)
 *
 * @author Attila Szegedi
 */
public class SoftCacheStorage implements ConcurrentCacheStorage
{
    private static final Method atomicRemove = getAtomicRemoveMethod();
    
    private final ReferenceQueue queue = new ReferenceQueue();
    private final Map map;
    private final boolean concurrent;
    
    public SoftCacheStorage() {
        this(_ConcurrentMapFactory.newMaybeConcurrentHashMap());
    }
    
    public boolean isConcurrent() {
        return concurrent;
    }
    
    public SoftCacheStorage(Map backingMap) {
        map = backingMap;
        this.concurrent = _ConcurrentMapFactory.isConcurrent(map);
    }
    
    public Object get(Object key) {
        processQueue();
        Reference ref = (Reference)map.get(key);
        return ref == null ? null : ref.get();
    }

    public void put(Object key, Object value) {
        processQueue();
        map.put(key, new SoftValueReference(key, value, queue));
    }

    public void remove(Object key) {
        processQueue();
        map.remove(key);
    }

    public void clear() {
        map.clear();
        processQueue();
    }

    private void processQueue() {
        for(;;) {
            SoftValueReference ref = (SoftValueReference)queue.poll();
            if(ref == null) {
                return;
            }
            Object key = ref.getKey();
            if(concurrent) {
                try {
                    atomicRemove.invoke(map, new Object[] { key, ref });
                }
                catch(IllegalAccessException e) {
                    throw new UndeclaredThrowableException(e);
                }
                catch(InvocationTargetException e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
            else if(map.get(key) == ref) {
                map.remove(key);
            }
        }
    }

    private static final class SoftValueReference extends SoftReference {
        private final Object key;

        SoftValueReference(Object key, Object value, ReferenceQueue queue) {
            super(value, queue);
            this.key = key;
        }

        Object getKey() {
            return key;
        }
    }
    
    private static Method getAtomicRemoveMethod() {
        try {
            return Class.forName("java.util.concurrent.ConcurrentMap").getMethod("remove", new Class[] { Object.class, Object.class });
        }
        catch(ClassNotFoundException e) {
            return null;
        }
        catch(NoSuchMethodException e) {
            throw new UndeclaredThrowableException(e);
        }
    }
}