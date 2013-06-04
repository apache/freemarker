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
import java.util.Collection;
import java.util.Iterator;

/**
 * A simple implementation of {@link TemplateCollectionModel}.
 * It's able to wrap <tt>java.util.Iterator</tt>-s and <tt>java.util.Collection</tt>-s.
 * If you wrap an <tt>Iterator</tt>, the variable can be &lt;list>-ed (&lt;forach>-ed) only once!
 *
 * <p>Consider using {@link SimpleSequence} instead of this class if you want to wrap <tt>Iterator</tt>s.
 * <tt>SimpleSequence</tt> will read all elements of the <tt>Iterator</tt>, and store them in a <tt>List</tt>
 * (this may cause too high resource consumption in some applications), so you can list the variable
 * for unlimited times. Also, if you want to wrap <tt>Collection</tt>s, and then list the resulting
 * variable for many times, <tt>SimpleSequence</tt> may gives better performance, as the
 * wrapping of non-<tt>TemplateModel</tt> objects happens only once.
 *
 * <p>This class is thread-safe. The returned <tt>TemplateModelIterator</tt>-s
 * are <em>not</em> thread-safe.
 */
public class SimpleCollection extends WrappingTemplateModel
implements TemplateCollectionModel, Serializable {
    
    private boolean iteratorDirty;
    private Iterator iterator;
    private Collection collection;

    public SimpleCollection(Iterator iterator) {
        this.iterator = iterator;
    }

    public SimpleCollection(Collection collection) {
        this.collection = collection;
    }

    public SimpleCollection(Iterator iterator, ObjectWrapper wrapper) {
        super(wrapper);
        this.iterator = iterator;
    }

    public SimpleCollection(Collection collection, ObjectWrapper wrapper) {
        super(wrapper);
        this.collection = collection;
    }

    /**
     * Retrieves a template model iterator that is used to iterate over the elements in this collection.
     *  
     * <p>When you wrap an <tt>Iterator</tt> and you get <tt>TemplateModelIterator</tt> for multiple times,
     * only on of the returned <tt>TemplateModelIterator</tt> instances can be really used. When you have called a
     * method of a <tt>TemplateModelIterator</tt> instance, all other instance will throw a
     * <tt>TemplateModelException</tt> when you try to call their methods, since the wrapped <tt>Iterator</tt>
     * can't return the first element.
     */
    public TemplateModelIterator iterator() {
        if (iterator != null) {
            return new SimpleTemplateModelIterator(iterator, true);
        } else {
            synchronized (collection) {
                return new SimpleTemplateModelIterator(collection.iterator(), false);
            }
        }
    }
    
    /*
     * An instance of this class must be accessed only from a single thread.
     * The encapsulated Iterator may accessible from multiple threads (as multiple
     * SimpleTemplateModelIterator instance can wrap the same Iterator instance),
     * but the first thread which uses the shared Iterator will monopolize that.
     */
    private class SimpleTemplateModelIterator implements TemplateModelIterator {
        
        private Iterator iterator;
        private boolean iteratorShared;
            
        SimpleTemplateModelIterator(Iterator iterator, boolean iteratorShared) {
            this.iterator = iterator;
            this.iteratorShared = iteratorShared;
        }

        public TemplateModel next() throws TemplateModelException {
            if (iteratorShared) makeIteratorDirty();
            
            if (!iterator.hasNext()) {
                throw new TemplateModelException("The collection has no more elements.");
            }
            
            Object value  = iterator.next();
            if (value instanceof TemplateModel) {
                return (TemplateModel) value;
            } else {
                return wrap(value);
            }
        }

        public boolean hasNext() throws TemplateModelException {
            /* 
             * Theorically this should not make the iterator dirty,
             * but I met sync. problems if I don't do it here. :(
             */
            if (iteratorShared) makeIteratorDirty();
            return iterator.hasNext();
        }
        
        private void makeIteratorDirty() throws TemplateModelException {
            synchronized (SimpleCollection.this) {
                if (iteratorDirty) {
                    throw new TemplateModelException(
                            "This collection variable wraps a java.util.Iterator, "
                            + "thus it can be <list>-ed or <foreach>-ed only once");
                } else {
                    iteratorDirty = true;
                    iteratorShared = false;
                }
            }
        }
    }
}
