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

package freemarker.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import freemarker.ext.beans.BeansWrapper;

/**
 * A simple implementation of the {@link TemplateSequenceModel} interface, using its own underlying {@link List} for
 * storing the list items. If you are wrapping an already existing {@link List} or {@code array}, you should certainly
 * use {@link DefaultMapAdapter} or {@link DefaultArrayAdapter} (see comparison below).
 * 
 * <p>
 * This class is thread-safe if you don't call modifying methods (like {@link #add(Object)}) after you have made the
 * object available for multiple threads (assuming you have published it safely to the other threads; see JSR-133 Java
 * Memory Model). These methods aren't called by FreeMarker, so it's usually not a concern.
 * 
 * <p>
 * <b>{@link SimpleSequence} VS {@link DefaultListAdapter}/{@link DefaultArrayAdapter} - Which to use when?</b>
 * </p>
 * 
 * <p>
 * For a {@link List} or {@code array} that exists regardless of FreeMarker, only you need to access it from templates,
 * {@link DefaultMapAdapter} should be the default choice, as it can be unwrapped to the originally wrapped object
 * (important when passing it to Java methods from the template). It also has more predictable performance (no spikes).
 * 
 * <p>
 * For a sequence that's made specifically to be used from templates, creating an empty {@link SimpleSequence} then
 * filling it with {@link SimpleSequence#add(Object)} is usually the way to go, as the resulting sequence is
 * significantly faster to read from templates than a {@link DefaultListAdapter} (though it's somewhat slower to read
 * from a plain Java method to which it had to be passed adapted to a {@link List}).
 * 
 * <p>
 * If regardless of which of the above two cases stand, you just need to (or more convenient to) create the sequence
 * from a {@link List} (via {@link DefaultListAdapter#adapt(List, freemarker.template.utility.RichObjectWrapper)} or
 * {@link SimpleSequence#SimpleSequence(Collection)}), which will be the faster depends on how many times will the
 * <em>same</em> {@link List} entry be read from the template(s) later, on average. If, on average, you read each entry
 * for more than 4 times, {@link SimpleSequence} will be most certainly faster, but if for 2 times or less (and
 * especially if not at all) then {@link DefaultMapAdapter} will be. Before choosing based on performance though, pay
 * attention to the behavioral differences; {@link SimpleSequence} will shallow-copy the original {@link List} at
 * construction time, so it won't reflect {@link List} content changes after the {@link SimpleSequence} construction,
 * also {@link SimpleSequence} can't be unwrapped to the original wrapped instance.
 *
 * @see DefaultListAdapter
 * @see DefaultArrayAdapter
 * @see TemplateSequenceModel
 */
public class SimpleSequence extends WrappingTemplateModel implements TemplateSequenceModel, Serializable {

    /**
     * The {@link List} that stored the elements of this sequence. It migth contains both {@link TemplateModel} elements
     * and non-{@link TemplateModel} elements.
     */
    protected final List list;
    
    private List unwrappedList;

    /**
     * Constructs an empty simple sequence that will use the the default object 
     * wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}.
     * 
     * @deprecated Use {@link #SimpleSequence(ObjectWrapper)} instead.
     */
    public SimpleSequence() {
        this((ObjectWrapper) null);
    }

    /**
     * Constructs an empty simple sequence with preallocated capacity and using
     * the default object wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}.
     * 
     * @deprecated Use {@link #SimpleSequence(Collection, ObjectWrapper)}.
     */
    public SimpleSequence(int capacity) {
        list = new ArrayList(capacity);
    }

    
    /**
     * Constructs a simple sequence that will contain the elements
     * from the specified {@link Collection} and will use the the default 
     * object wrapper set in 
     * {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}.
     * @param collection the collection containing initial values. Note that a
     * copy of the collection is made for internal use.
     * 
     * @deprecated Use {@link #SimpleSequence(Collection, ObjectWrapper)}.
     */
    public SimpleSequence(Collection collection) {
        this(collection, null);
    }
    
    /**
     * Constructs a simple sequence from the passed collection model, which shouldn't be added to later. The internal
     * list will be build immediately (not lazily). The resulting sequence shouldn't be extended with
     * {@link #add(Object)}, because the appropriate {@link ObjectWrapper} won't be available; use
     * {@link #SimpleSequence(Collection, ObjectWrapper)} instead, if you need that.
     */
    public SimpleSequence(TemplateCollectionModel tcm) throws TemplateModelException {
        ArrayList alist = new ArrayList();
        for (TemplateModelIterator it = tcm.iterator(); it.hasNext();) {
            alist.add(it.next());
        }
        alist.trimToSize();
        list = alist;
    }

    /**
     * Constructs an empty sequence using the specified object wrapper.
     * 
     * @param wrapper
     *            The object wrapper to use to wrap the list items into {@link TemplateModel} instances. {@code null} is
     *            allowed, but deprecated, and will cause the deprecated default object wrapper (set in
     *            {@link WrappingTemplateModel#setDefaultObjectWrapper(ObjectWrapper)}) to be used.
     */
    public SimpleSequence(ObjectWrapper wrapper) {
        super(wrapper);
        list = new ArrayList();
    }
    
    /**
     * Constructs an empty simple sequence with preallocated capacity.
     * 
     * @param wrapper
     *            See the similar parameter of {@link SimpleSequence#SimpleSequence(ObjectWrapper)}.
     * 
     * @since 2.3.21
     */
    public SimpleSequence(int capacity, ObjectWrapper wrapper) {
        super(wrapper);
        list = new ArrayList(capacity);
    }    
    
    /**
     * Constructs a simple sequence that will contain the elements from the specified {@link Collection}; consider
     * using {@link DefaultListAdapter} instead.
     * 
     * @param collection
     *            The collection containing the initial items of this sequence. A shalow copy of this collection is made
     *            immediately for internal use (thus, later modification on the parameter collection won't be visible in
     *            the resulting sequence). The items however, will be only wrapped with the {@link ObjectWrapper}
     *            lazily, when first needed.
     * @param wrapper
     *            See the similar parameter of {@link SimpleSequence#SimpleSequence(ObjectWrapper)}.
     */
    public SimpleSequence(Collection collection, ObjectWrapper wrapper) {
        super(wrapper);
        list = new ArrayList(collection);
    }

    /**
     * Adds an arbitrary object to the end of this sequence. If the newly added object does not implement the
     * {@link TemplateModel} interface, it will be wrapped into the appropriate {@link TemplateModel} interface when
     * it's first read (lazily).
     *
     * @param obj
     *            The object to be added.
     */
    public void add(Object obj) {
        list.add(obj);
        unwrappedList = null;
    }

    /**
     * Adds a boolean value to the end of this sequence. The newly added boolean will be immediately converted into
     * {@link TemplateBooleanModel#TRUE} or {@link TemplateBooleanModel#FALSE}, without using the {@link ObjectWrapper}.
     *
     * @param b
     *            The boolean value to be added.
     * 
     * @deprecated Use {@link #add(Object)} instead, as this bypasses the {@link ObjectWrapper}.
     */
    public void add(boolean b) {
        add(b ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE);
    }
    
    /**
     * Builds a deep-copy of the underlying list, unwrapping any values that were already converted to
     * {@link TemplateModel}-s. When called for the second time (or later), it just reuses the first result, unless the
     * sequence was modified since then.
     * 
     * @deprecated No replacement exists; not a reliable way of getting back the original list elemnts.
     */
    public List toList() throws TemplateModelException {
        if (unwrappedList == null) {
            Class listClass = list.getClass();
            List result = null;
            try {
                result = (List) listClass.newInstance();
            } catch (Exception e) {
                throw new TemplateModelException("Error instantiating an object of type " + listClass.getName(),
                        e);
            }
            BeansWrapper bw = BeansWrapper.getDefaultInstance();
            for (int i = 0; i < list.size(); i++) {
                Object elem = list.get(i);
                if (elem instanceof TemplateModel) {
                    elem = bw.unwrap((TemplateModel) elem);
                }
                result.add(elem);
            }
            unwrappedList = result;
        }
        return unwrappedList;
    }
    
    /**
     * Returns the item at the specified index of the list. If the item isn't yet an {@link TemplateModel}, it will wrap
     * it to one now, and writes it back into the backing list.
     */
    public TemplateModel get(int index) throws TemplateModelException {
        try {
            Object value = list.get(index);
            if (value instanceof TemplateModel) {
                return (TemplateModel) value;
            }
            TemplateModel tm = wrap(value);
            list.set(index, tm);
            return tm;
        }
        catch(IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int size() {
        return list.size();
    }

    /**
     * @return a synchronized wrapper for list.
     */
    public SimpleSequence synchronizedWrapper() {
        return new SynchronizedSequence();
    }
    
    public String toString() {
        return list.toString();
    }

    private class SynchronizedSequence extends SimpleSequence {

        public void add(Object obj) {
            synchronized (SimpleSequence.this) {
                SimpleSequence.this.add(obj);
            }
        }

        public TemplateModel get(int i) throws TemplateModelException {
            synchronized (SimpleSequence.this) {
                return SimpleSequence.this.get(i);
            }
        }
        
        public int size() {
            synchronized (SimpleSequence.this) {
                return SimpleSequence.this.size();
            }
        }
        
        public List toList() throws TemplateModelException {
            synchronized (SimpleSequence.this) {
                return SimpleSequence.this.toList();
            }
        }
    }
    
}