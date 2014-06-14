/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    
    /** @since 2.3.21 */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[] { };

    /** @since 2.3.21 */
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[] { };;

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
