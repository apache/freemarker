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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.WrappingTemplateModel;

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
 * It also matters if for how many times will the <em>same</em> {@link List} entry be read from the template(s) later,
 * on average. If, on average, you read each entry for more than 4 times, {@link SimpleSequence} will be most
 * certainly faster, but if for 2 times or less (and especially if not at all) then {@link DefaultMapAdapter} will
 * be faster. Before choosing based on performance though, pay attention to the behavioral differences;
 * {@link SimpleSequence} will shallow-copy the original {@link List} at construction time, so it won't reflect
 * {@link List} content changes after the {@link SimpleSequence} construction, also {@link SimpleSequence} can't be
 * unwrapped to the original wrapped instance.
 *
 * @see DefaultListAdapter
 * @see DefaultArrayAdapter
 * @see TemplateSequenceModel
 */
public class SimpleSequence extends WrappingTemplateModel implements TemplateSequenceModel {

    /**
     * The {@link List} that stored the elements of this sequence. It might contains both {@link TemplateModel} elements
     * and non-{@link TemplateModel} elements.
     */
    protected final List list;

    /**
     * Constructs an empty sequence using the specified object wrapper.
     * 
     * @param wrapper
     *            The object wrapper to use to wrap the list items into {@link TemplateModel} instances. Not
     *            {@code null}.
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
     *            The collection containing the initial items of the sequence. A shallow copy of this collection is made
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
    }

    /**
     * Returns the item at the specified index of the list. If the item isn't yet an {@link TemplateModel}, it will wrap
     * it to one now, and writes it back into the backing list.
     */
    @Override
    public TemplateModel get(int index) throws TemplateException {
        if (index >= list.size() || index < 0) {
            return null;
        }

        Object value = list.get(index);
        if (value instanceof TemplateModel) {
            return (TemplateModel) value;
        }
        TemplateModel tm = wrap(value);
        list.set(index, tm);
        return tm;
    }

    @Override
    public int getCollectionSize() {
        return list.size();
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return list.isEmpty();
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new SequenceTemplateModelIterator(this);
    }

    @Override
    public String toString() {
        return list.toString();
    }

}