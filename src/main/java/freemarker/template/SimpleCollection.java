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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * A simple implementation of {@link TemplateCollectionModel}.
 * It's able to wrap <tt>java.util.Iterator</tt>-s and <tt>java.util.Collection</tt>-s.
 * If you wrap an <tt>Iterator</tt>, the variable can be &lt;#list&gt;-ed only once!
 *
 * <p>Consider using {@link SimpleSequence} instead of this class if you want to wrap <tt>Iterator</tt>s.
 * <tt>SimpleSequence</tt> will read all elements of the <tt>Iterator</tt>, and store them in a <tt>List</tt>
 * (this may cause too high resource consumption in some applications), so you can list the variable
 * for unlimited times. Also, if you want to wrap <tt>Collection</tt>s, and then list the resulting
 * variable for many times, <tt>SimpleSequence</tt> may gives better performance, as the
 * wrapping of non-<tt>TemplateModel</tt> objects happens only once.
 *
 * <p>This class is thread-safe. The returned {@link TemplateModelIterator}-s
 * are <em>not</em> thread-safe.
 */
public class SimpleCollection extends WrappingTemplateModel
implements TemplateCollectionModel, Serializable {
    
    private boolean iteratorOwned;
    private final Iterator iterator;
    private final Iterable iterable;

    /**
     * @deprecated Use {@link #SimpleCollection(Iterator, ObjectWrapper)}
     */
    @Deprecated
    public SimpleCollection(Iterator iterator) {
        this.iterator = iterator;
        iterable = null;
    }

    /**
     * @deprecated Use {@link #SimpleCollection(Iterable, ObjectWrapper)} instead.
     */
    @Deprecated
    public SimpleCollection(Iterable iterable) {
        this.iterable = iterable;
        iterator = null;
    }

    /**
     * Same as {@link SimpleCollection#SimpleCollection(Iterable)}; kept for binary compatibility.
     * 
     * @deprecated Use {@link #SimpleCollection(Iterable, ObjectWrapper)} instead.
     */
    @Deprecated
    public SimpleCollection(Collection collection) {
        this((Iterable) collection);
    }
    
    /**
     * Same as {@link SimpleCollection#SimpleCollection(Iterable, ObjectWrapper)}; kept for binary compatibility.
     */
    public SimpleCollection(Collection collection, ObjectWrapper wrapper) {
        this((Iterable) collection, wrapper);
    }
    
    public SimpleCollection(Iterator iterator, ObjectWrapper wrapper) {
        super(wrapper);
        this.iterator = iterator;
        iterable = null;
    }

    /**
     * @param iterable Note that this was a {@link Collection} before 2.3.27, not an {@link Iterable}
     */
    public SimpleCollection(Iterable iterable, ObjectWrapper wrapper) {
        super(wrapper);
        this.iterable = iterable;
        iterator = null;
    }

    /**
     * Retrieves a template model iterator that is used to iterate over the elements in this collection.
     *  
     * <p>When you wrap an <tt>Iterator</tt> and you get <tt>TemplateModelIterator</tt> for multiple times,
     * only on of the returned <tt>TemplateModelIterator</tt> instances can be really used. When you have called a
     * method of a <tt>TemplateModelIterator</tt> instance, all other instance will throw a
     * <tt>TemplateModelException</tt> when you try to call their methods, since the wrapped <tt>Iterator</tt>
     * can't return the first element anymore.
     */
    @Override
    public TemplateModelIterator iterator() {
        return iterator != null
                ? new SimpleTemplateModelIterator(iterator, false)
                : new SimpleTemplateModelIterator(iterable.iterator(), true);
    }
    
    /**
     * Wraps an {@link Iterator}; not thread-safe. The encapsulated {@link Iterator} may be accessible from multiple
     * threads (as multiple {@link SimpleTemplateModelIterator} instance can wrap the same {@link Iterator} instance),
     * but if the {@link Iterator} was marked in the constructor as shared, the first thread which uses the
     * {@link Iterator} will monopolize that.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {
        
        private final Iterator iterator;
        private boolean iteratorOwnedByMe;
            
        SimpleTemplateModelIterator(Iterator iterator, boolean iteratorOwnedByMe) {
            this.iterator = iterator;
            this.iteratorOwnedByMe = iteratorOwnedByMe;
        }

        @Override
        public TemplateModel next() throws TemplateModelException {
            if (!iteratorOwnedByMe) { 
                synchronized (SimpleCollection.this) {
                    checkIteratorOwned();
                    iteratorOwned = true;
                    iteratorOwnedByMe = true;
                }
            }
            
            if (!iterator.hasNext()) {
                throw new TemplateModelException("The collection has no more items.");
            }
            
            Object value  = iterator.next();
            return value instanceof TemplateModel ? (TemplateModel) value : wrap(value);
        }

        @Override
        public boolean hasNext() throws TemplateModelException {
            // Calling hasNext may looks safe, but I have met sync. problems.
            if (!iteratorOwnedByMe) {
                synchronized (SimpleCollection.this) {
                    checkIteratorOwned();
                }
            }
            
            return iterator.hasNext();
        }
        
        private void checkIteratorOwned() throws TemplateModelException {
            if (iteratorOwned) {
                throw new TemplateModelException(
                        "This collection value wraps a java.util.Iterator, thus it can be listed only once.");
            }
        }
        
    }
    
}
