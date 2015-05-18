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

package freemarker.ext.util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A variant of {@link java.util.HashMap} that uses
 * {@link System#identityHashCode(Object)} for hashing, and reference comparison
 * instead of {@link Object#equals(Object)}. Note that this applies only to keys,
 * and not to values, i.e. {@link #containsValue(Object)} still uses {@link Object#equals(Object)}.
 */
public class IdentityHashMap
    extends AbstractMap
    implements Map, Cloneable, java.io.Serializable
{

    public static final long serialVersionUID = 362498820763181265L;
    /**
     * The hash table data.
     */
    private transient Entry table[];

    /**
     * The total number of mappings in the hash table.
     */
    private transient int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     */
    private float loadFactor;

    /**
     * The number of times this IdentityHashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the IdentityHashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the IdentityHashMap fail-fast.  (See ConcurrentModificationException).
     */
    private transient int modCount = 0;

    /**
     * Constructs a new, empty map with the specified initial
     * capacity and the specified load factor.
     *
     * @param      initialCapacity   the initial capacity of the IdentityHashMap.
     * @param      loadFactor        the load factor of the IdentityHashMap
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive.
     */
    public IdentityHashMap(int initialCapacity, float loadFactor)
    {
        if (initialCapacity < 0)
            throw new IllegalArgumentException(
                "Illegal Initial Capacity: " + initialCapacity);
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException(
                "Illegal Load factor: " + loadFactor);
        if (initialCapacity == 0)
            initialCapacity = 1;
        this.loadFactor = loadFactor;
        table = new Entry[initialCapacity];
        threshold = (int) (initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity
     * and default load factor, which is <tt>0.75</tt>.
     *
     * @param   initialCapacity   the initial capacity of the IdentityHashMap.
     * @throws    IllegalArgumentException if the initial capacity is less
     *              than zero.
     */
    public IdentityHashMap(int initialCapacity)
    {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty map with a default capacity and load
     * factor, which is <tt>0.75</tt>.
     */
    public IdentityHashMap()
    {
        this(11, 0.75f);
    }

    /**
     * Constructs a new map with the same mappings as the given map.  The
     * map is created with a capacity of twice the number of mappings in
     * the given map or 11 (whichever is greater), and a default load factor,
     * which is <tt>0.75</tt>.
     *
     * @param t the map whose mappings are to be placed in this map.
     */
    public IdentityHashMap(Map t)
    {
        this(Math.max(2 * t.size(), 11), 0.75f);
        putAll(t);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size()
    {
        return count;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty()
    {
        return count == 0;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value)
    {
        Entry tab[] = table;

        if (value == null)
        {
            for (int i = tab.length; i-- > 0;)
                for (Entry e = tab[i]; e != null; e = e.next)
                    if (e.value == null)
                        return true;
        }
        else
        {
            for (int i = tab.length; i-- > 0;)
                for (Entry e = tab[i]; e != null; e = e.next)
                    if (value.equals(e.value))
                        return true;
        }

        return false;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     * @param key key whose presence in this Map is to be tested.
     */
    public boolean containsKey(Object key)
    {
        Entry tab[] = table;
        if (key != null)
        {
            int hash = System.identityHashCode(key);
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; e != null; e = e.next)
                if (e.hash == hash && key == e.key)
                    return true;
        }
        else
        {
            for (Entry e = tab[0]; e != null; e = e.next)
                if (e.key == null)
                    return true;
        }

        return false;
    }

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * <tt>null</tt> if the map contains no mapping for this key.  A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @return the value to which this map maps the specified key.
     * @param key key whose associated value is to be returned.
     */
    public Object get(Object key)
    {
        Entry tab[] = table;

        if (key != null)
        {
            int hash = System.identityHashCode(key);
            int index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; e != null; e = e.next)
                if ((e.hash == hash) && key == e.key)
                    return e.value;
        }
        else
        {
            for (Entry e = tab[0]; e != null; e = e.next)
                if (e.key == null)
                    return e.value;
        }

        return null;
    }

    /**
     * Rehashes the contents of this map into a new <tt>IdentityHashMap</tt> instance
     * with a larger capacity. This method is called automatically when the
     * number of keys in this map exceeds its capacity and load factor.
     */
    private void rehash()
    {
        int oldCapacity = table.length;
        Entry oldMap[] = table;

        int newCapacity = oldCapacity * 2 + 1;
        Entry newMap[] = new Entry[newCapacity];

        modCount++;
        threshold = (int) (newCapacity * loadFactor);
        table = newMap;

        for (int i = oldCapacity; i-- > 0;)
        {
            for (Entry old = oldMap[i]; old != null;)
            {
                Entry e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the IdentityHashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    public Object put(Object key, Object value)
    {
        // Makes sure the key is not already in the IdentityHashMap.
        Entry tab[] = table;
        int hash = 0;
        int index = 0;

        if (key != null)
        {
            hash = System.identityHashCode(key);
            index = (hash & 0x7FFFFFFF) % tab.length;
            for (Entry e = tab[index]; e != null; e = e.next)
            {
                if ((e.hash == hash) && key == e.key)
                {
                    Object old = e.value;
                    e.value = value;
                    return old;
                }
            }
        }
        else
        {
            for (Entry e = tab[0]; e != null; e = e.next)
            {
                if (e.key == null)
                {
                    Object old = e.value;
                    e.value = value;
                    return old;
                }
            }
        }

        modCount++;
        if (count >= threshold)
        {
            // Rehash the table if the threshold is exceeded
            rehash();

            tab = table;
            index = (hash & 0x7FFFFFFF) % tab.length;
        }

        // Creates the new entry.
        Entry e = new Entry(hash, key, value, tab[index]);
        tab[index] = e;
        count++;
        return null;
    }

    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    public Object remove(Object key)
    {
        Entry tab[] = table;

        if (key != null)
        {
            int hash = System.identityHashCode(key);
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (Entry e = tab[index], prev = null;
                e != null;
                prev = e, e = e.next)
            {
                if ((e.hash == hash) && key == e.key)
                {
                    modCount++;
                    if (prev != null)
                        prev.next = e.next;
                    else
                        tab[index] = e.next;

                    count--;
                    Object oldValue = e.value;
                    e.value = null;
                    return oldValue;
                }
            }
        }
        else
        {
            for (Entry e = tab[0], prev = null;
                e != null;
                prev = e, e = e.next)
            {
                if (e.key == null)
                {
                    modCount++;
                    if (prev != null)
                        prev.next = e.next;
                    else
                        tab[0] = e.next;

                    count--;
                    Object oldValue = e.value;
                    e.value = null;
                    return oldValue;
                }
            }
        }

        return null;
    }

    /**
     * Copies all of the mappings from the specified map to this one.
     *
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified Map.
     *
     * @param t Mappings to be stored in this map.
     */
    public void putAll(Map t)
    {
        Iterator i = t.entrySet().iterator();
        while (i.hasNext())
        {
            Map.Entry e = (Map.Entry) i.next();
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear()
    {
        Entry tab[] = table;
        modCount++;
        for (int index = tab.length; --index >= 0;)
            tab[index] = null;
        count = 0;
    }

    /**
     * Returns a shallow copy of this <tt>IdentityHashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    public Object clone()
    {
        try
        {
            IdentityHashMap t = (IdentityHashMap) super.clone();
            t.table = new Entry[table.length];
            for (int i = table.length; i-- > 0;)
            {
                t.table[i] =
                    (table[i] != null) ? (Entry) table[i].clone() : null;
            }
            t.keySet = null;
            t.entrySet = null;
            t.values = null;
            t.modCount = 0;
            return t;
        }
        catch (CloneNotSupportedException e)
        {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    // Views

    private transient Set keySet = null;
    private transient Set entrySet = null;
    private transient Collection values = null;

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set keySet()
    {
        if (keySet == null)
        {
            keySet = new AbstractSet()
            {
                public Iterator iterator()
                {
                    return getHashIterator(KEYS);
                }
                public int size()
                {
                    return count;
                }
                public boolean contains(Object o)
                {
                    return containsKey(o);
                }
                public boolean remove(Object o)
                {
                    int oldSize = count;
                    IdentityHashMap.this.remove(o);
                    return count != oldSize;
                }
                public void clear()
                {
                    IdentityHashMap.this.clear();
                }
            };
        }
        return keySet;
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection values()
    {
        if (values == null)
        {
            values = new AbstractCollection()
            {
                public Iterator iterator()
                {
                    return getHashIterator(VALUES);
                }
                public int size()
                {
                    return count;
                }
                public boolean contains(Object o)
                {
                    return containsValue(o);
                }
                public void clear()
                {
                    IdentityHashMap.this.clear();
                }
            };
        }
        return values;
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see java.util.Map.Entry
     */
    public Set entrySet()
    {
        if (entrySet == null)
        {
            entrySet = new AbstractSet()
            {
                public Iterator iterator()
                {
                    return getHashIterator(ENTRIES);
                }

                public boolean contains(Object o)
                {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry entry = (Map.Entry) o;
                    Object key = entry.getKey();
                    Entry tab[] = table;
                    int hash = (key == null ? 0 : System.identityHashCode(key));
                    int index = (hash & 0x7FFFFFFF) % tab.length;

                    for (Entry e = tab[index]; e != null; e = e.next)
                        if (e.hash == hash && e.equals(entry))
                            return true;
                    return false;
                }

                public boolean remove(Object o)
                {
                    if (!(o instanceof Map.Entry))
                        return false;
                    Map.Entry entry = (Map.Entry) o;
                    Object key = entry.getKey();
                    Entry tab[] = table;
                    int hash = (key == null ? 0 : System.identityHashCode(key));
                    int index = (hash & 0x7FFFFFFF) % tab.length;

                    for (Entry e = tab[index], prev = null;
                        e != null;
                        prev = e, e = e.next)
                    {
                        if (e.hash == hash && e.equals(entry))
                        {
                            modCount++;
                            if (prev != null)
                                prev.next = e.next;
                            else
                                tab[index] = e.next;

                            count--;
                            e.value = null;
                            return true;
                        }
                    }
                    return false;
                }

                public int size()
                {
                    return count;
                }

                public void clear()
                {
                    IdentityHashMap.this.clear();
                }
            };
        }

        return entrySet;
    }

    private Iterator getHashIterator(int type)
    {
        if (count == 0)
        {
            return emptyHashIterator;
        }
        else
        {
            return new HashIterator(type);
        }
    }

    /**
     * IdentityHashMap collision list entry.
     */
    private static class Entry implements Map.Entry
    {
        int hash;
        Object key;
        Object value;
        Entry next;

        Entry(int hash, Object key, Object value, Entry next)
        {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        protected Object clone()
        {
            return new Entry(
                hash,
                key,
                value,
                (next == null ? null : (Entry) next.clone()));
        }

        // Map.Entry Ops

        public Object getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public Object setValue(Object value)
        {
            Object oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry) o;

            return (key == e.getKey())
                && (value == null
                    ? e.getValue() == null
                    : value.equals(e.getValue()));
        }

        public int hashCode()
        {
            return hash ^ (value == null ? 0 : value.hashCode());
        }

        public String toString()
        {
            return key + "=" + value;
        }
    }

    // Types of Iterators
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    private static EmptyHashIterator emptyHashIterator =
        new EmptyHashIterator();

    private static class EmptyHashIterator implements Iterator
    {

        EmptyHashIterator()
        {

        }

        public boolean hasNext()
        {
            return false;
        }

        public Object next()
        {
            throw new NoSuchElementException();
        }

        public void remove()
        {
            throw new IllegalStateException();
        }

    }

    private class HashIterator implements Iterator
    {
        Entry[] table = IdentityHashMap.this.table;
        int index = table.length;
        Entry entry = null;
        Entry lastReturned = null;
        int type;

        /**
         * The modCount value that the iterator believes that the backing
         * List should have.  If this expectation is violated, the iterator
         * has detected concurrent modification.
         */
        private int expectedModCount = modCount;

        HashIterator(int type)
        {
            this.type = type;
        }

        public boolean hasNext()
        {
            Entry e = entry;
            int i = index;
            Entry t[] = table;
            /* Use locals for faster loop iteration */
            while (e == null && i > 0)
                e = t[--i];
            entry = e;
            index = i;
            return e != null;
        }

        public Object next()
        {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            Entry et = entry;
            int i = index;
            Entry t[] = table;

            /* Use locals for faster loop iteration */
            while (et == null && i > 0)
                et = t[--i];

            entry = et;
            index = i;
            if (et != null)
            {
                Entry e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? e.key : (type == VALUES ? e.value : e);
            }
            throw new NoSuchElementException();
        }

        public void remove()
        {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            Entry[] tab = IdentityHashMap.this.table;
            int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

            for (Entry e = tab[index], prev = null;
                e != null;
                prev = e, e = e.next)
            {
                if (e == lastReturned)
                {
                    modCount++;
                    expectedModCount++;
                    if (prev == null)
                        tab[index] = e.next;
                    else
                        prev.next = e.next;
                    count--;
                    lastReturned = null;
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Save the state of the <tt>IdentityHashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the IdentityHashMap (the length of the
     *		   bucket array) is emitted (int), followed  by the
     *		   <i>size</i> of the IdentityHashMap (the number of key-value
     *		   mappings), followed by the key (Object) and value (Object)
     *		   for each key-value mapping represented by the IdentityHashMap
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException
    {
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();

        // Write out number of buckets
        s.writeInt(table.length);

        // Write out size (number of Mappings)
        s.writeInt(count);

        // Write out keys and values (alternating)
        for (int index = table.length - 1; index >= 0; index--)
        {
            Entry entry = table[index];

            while (entry != null)
            {
                s.writeObject(entry.key);
                s.writeObject(entry.value);
                entry = entry.next;
            }
        }
    }

    /**
     * Reconstitute the <tt>IdentityHashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException
    {
        // Read in the threshold, loadfactor, and any hidden stuff
        s.defaultReadObject();

        // Read in number of buckets and allocate the bucket array;
        int numBuckets = s.readInt();
        table = new Entry[numBuckets];

        // Read in size (number of Mappings)
        int size = s.readInt();

        // Read the keys and values, and put the mappings in the IdentityHashMap
        for (int i = 0; i < size; i++)
        {
            Object key = s.readObject();
            Object value = s.readObject();
            put(key, value);
        }
    }

    int capacity()
    {
        return table.length;
    }

    float loadFactor()
    {
        return loadFactor;
    }
}
