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

package freemarker.template.utility;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of missing JDK 1.3 collection features for JDK 1.2
 * @author Attila Szegedi
 */
public class Collections12
{
    public static final Map EMPTY_MAP = new EmptyMap();

    private Collections12()
    {
    }

    private static final class EmptyMap
        extends AbstractMap
        implements Serializable
    {
        public int size()
        {
            return 0;
        }

        public boolean isEmpty()
        {
            return true;
        }

        public boolean containsKey(Object key)
        {
            return false;
        }

        public boolean containsValue(Object value)
        {
            return false;
        }

        public Object get(Object key)
        {
            return null;
        }

        public Set keySet()
        {
            return Collections.EMPTY_SET;
        }

        public Collection values()
        {
            return Collections.EMPTY_SET;
        }

        public Set entrySet()
        {
            return Collections.EMPTY_SET;
        }

        public boolean equals(Object o)
        {
            return (o instanceof Map) && ((Map) o).size() == 0;
        }

        public int hashCode()
        {
            return 0;
        }
    }

    public static Map singletonMap(Object key, Object value)
    {
        return new SingletonMap(key, value);
    }

    private static class SingletonMap
        extends AbstractMap
        implements Serializable
    {
        private final Object k, v;

        SingletonMap(Object key, Object value)
        {
            k = key;
            v = value;
        }

        public int size()
        {
            return 1;
        }

        public boolean isEmpty()
        {
            return false;
        }

        public boolean containsKey(Object key)
        {
            return eq(key, k);
        }

        public boolean containsValue(Object value)
        {
            return eq(value, v);
        }

        public Object get(Object key)
        {
            return (eq(key, k) ? v : null);
        }

        private transient Set keySet = null;
        private transient Set entrySet = null;
        private transient Collection values = null;

        public Set keySet()
        {
            if (keySet == null)
                keySet = Collections.singleton(k);
            return keySet;
        }

        public Set entrySet()
        {
            if (entrySet == null)
                entrySet = Collections.singleton(new ImmutableEntry(k, v));
            return entrySet;
        }

        public Collection values()
        {
            if (values == null)
                values = Collections.singleton(v);
            return values;
        }

        private static class ImmutableEntry implements Map.Entry
        {
            final Object k;
            final Object v;

            ImmutableEntry(Object key, Object value)
            {
                k = key;
                v = value;
            }

            public Object getKey()
            {
                return k;
            }

            public Object getValue()
            {
                return v;
            }

            public Object setValue(Object value)
            {
                throw new UnsupportedOperationException();
            }

            public boolean equals(Object o)
            {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry e = (Map.Entry) o;
                return eq(e.getKey(), k) && eq(e.getValue(), v);
            }

            public int hashCode()
            {
                return (
                    (k == null ? 0 : k.hashCode())
                        ^ (v == null ? 0 : v.hashCode()));
            }

            public String toString()
            {
                return k + "=" + v;
            }
        }
    }

    public static List singletonList(Object o)
    {
        return new SingletonList(o);
    }

    private static class SingletonList
        extends AbstractList
        implements Serializable
    {
        private final Object element;

        SingletonList(Object obj)
        {
            element = obj;
        }

        public int size()
        {
            return 1;
        }

        public boolean contains(Object obj)
        {
            return eq(obj, element);
        }

        public Object get(int index)
        {
            if (index != 0)
                throw new IndexOutOfBoundsException(
                    "Index: " + index + ", Size: 1");
            return element;
        }
    }

    private static boolean eq(Object o1, Object o2)
    {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }
}
