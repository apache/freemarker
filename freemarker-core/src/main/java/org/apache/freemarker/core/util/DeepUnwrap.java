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

package org.apache.freemarker.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.AdapterTemplateModel;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePair;
import org.apache.freemarker.core.model.TemplateHashModelEx.KeyValuePairIterator;
import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateSequenceModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.WrapperTemplateModel;

/**
 * Utility methods for unwrapping {@link TemplateModel}-s.
 */
// TODO [FM3] Has to be changed or removed. For starters, for Collection-s and Map-s we should use adapters.
public class DeepUnwrap {
    /**
     * Unwraps {@link TemplateModel}-s recursively.
     * The converting of the {@link TemplateModel} object happens with the following rules:
     * <ol>
     *   <li>If the object implements {@link AdapterTemplateModel}, then the result
     *       of {@link AdapterTemplateModel#getAdaptedObject(Class)} for <tt>Object.class</tt> is returned.
     *   <li>If the object implements {@link WrapperTemplateModel}, then the result
     *       of {@link WrapperTemplateModel#getWrappedObject()} is returned.
     *   <li>If the object is identical to the null model of the current object 
     *       wrapper, null is returned. 
     *   <li>If the object implements {@link TemplateStringModel}, then the result
     *       of {@link TemplateStringModel#getAsString()} is returned.
     *   <li>If the object implements {@link TemplateNumberModel}, then the result
     *       of {@link TemplateNumberModel#getAsNumber()} is returned.
     *   <li>If the object implements {@link TemplateDateModel}, then the result
     *       of {@link TemplateDateModel#getAsDate()} is returned.
     *   <li>If the object implements {@link TemplateBooleanModel}, then the result
     *       of {@link TemplateBooleanModel#getAsBoolean()} is returned.
     *   <li>If the object implements {@link TemplateSequenceModel} or
     *       {@link TemplateIterableModel}, then a <code>java.util.ArrayList</code> is
     *       constructed from the subvariables, and each subvariable is unwrapped with
     *       the rules described here (recursive unwrapping).
     *   <li>If the object implements {@link TemplateHashModelEx}, then a
     *       <code>java.util.HashMap</code> is constructed from the subvariables, and each
     *       subvariable is unwrapped with the rules described here (recursive unwrapping).
     *   <li>Throw a {@link TemplateException}, because it doesn't know how to
     *       unwrap the object.
     * </ol>
     */
    public static Object unwrap(TemplateModel model) throws TemplateException {
        return unwrap(model, false);
    }

    /**
     * Same as {@link #unwrap(TemplateModel)}, but it doesn't throw exception 
     * if it doesn't know how to unwrap the model, but rather returns it as-is.
     */
    public static Object permissiveUnwrap(TemplateModel model) throws TemplateException {
        return unwrap(model, true);
    }
    
    private static Object unwrap(TemplateModel model, boolean permissive) throws TemplateException {
        Environment env = Environment.getCurrentEnvironment();
        TemplateModel nullModel = null;
        if (env != null) {
            ObjectWrapper wrapper = env.getObjectWrapper();
            if (wrapper != null) {
                nullModel = wrapper.wrap(null);
            }
        }
        return unwrap(model, nullModel, permissive);
    }

    private static Object unwrap(TemplateModel model, TemplateModel nullModel, boolean permissive) throws TemplateException {
        if (model instanceof AdapterTemplateModel) {
            return ((AdapterTemplateModel) model).getAdaptedObject(Object.class);
        }
        if (model instanceof WrapperTemplateModel) {
            return ((WrapperTemplateModel) model).getWrappedObject();
        }
        if (model == nullModel) {
            return null;
        }
        if (model instanceof TemplateStringModel) {
            return ((TemplateStringModel) model).getAsString();
        }
        if (model instanceof TemplateNumberModel) {
            return ((TemplateNumberModel) model).getAsNumber();
        }
        if (model instanceof TemplateDateModel) {
            return ((TemplateDateModel) model).getAsDate();
        }
        if (model instanceof TemplateBooleanModel) {
            return Boolean.valueOf(((TemplateBooleanModel) model).getAsBoolean());
        }
        if (model instanceof TemplateSequenceModel) {
            TemplateSequenceModel seq = (TemplateSequenceModel) model;
            int size = seq.getCollectionSize();
            ArrayList list = new ArrayList(size);
            TemplateModelIterator iter = seq.iterator();
            for (int i = 0; i < size; ++i) {
                list.add(unwrap(iter.next(), nullModel, permissive));
            }
            return list;
        }
        if (model instanceof TemplateIterableModel) {
            TemplateIterableModel coll = (TemplateIterableModel) model;
            ArrayList list = new ArrayList();
            TemplateModelIterator it = coll.iterator();            
            while (it.hasNext()) {
                list.add(unwrap(it.next(), nullModel, permissive));
            }
            return list;
        }
        if (model instanceof TemplateHashModelEx) {
            TemplateHashModelEx hash = (TemplateHashModelEx) model;
            if (hash.isEmptyHash()) {
                return Collections.emptyMap();
            }
            Map<Object, Object> map = new LinkedHashMap<Object, Object>((hash.getHashSize() + 1) * 4 / 3, 0.75f);
            KeyValuePairIterator kvps = hash.keyValuePairIterator();
            while (kvps.hasNext()) {
                KeyValuePair kvp = kvps.next();
                map.put(
                        unwrap(kvp.getKey(), nullModel, permissive),
                        unwrap(kvp.getValue(), nullModel, permissive));
            }
            return map;
        }
        if (permissive) {
            return model;
        }
        throw new TemplateException("Cannot deep-unwrap model of type " + model.getClass().getName());
    }
}