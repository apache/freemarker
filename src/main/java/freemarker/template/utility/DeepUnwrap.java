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

package freemarker.template.utility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateHashModelEx2;
import freemarker.template.TemplateHashModelEx2.KeyValuePair;
import freemarker.template.TemplateHashModelEx2.KeyValuePairIterator;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Utility methods for unwrapping {@link TemplateModel}-s.
 */
public class DeepUnwrap {
    private static final Class OBJECT_CLASS = Object.class;
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
     *   <li>If the object implements {@link TemplateScalarModel}, then the result
     *       of {@link TemplateScalarModel#getAsString()} is returned.
     *   <li>If the object implements {@link TemplateNumberModel}, then the result
     *       of {@link TemplateNumberModel#getAsNumber()} is returned.
     *   <li>If the object implements {@link TemplateDateModel}, then the result
     *       of {@link TemplateDateModel#getAsDate()} is returned.
     *   <li>If the object implements {@link TemplateBooleanModel}, then the result
     *       of {@link TemplateBooleanModel#getAsBoolean()} is returned.
     *   <li>If the object implements {@link TemplateSequenceModel} or
     *       {@link TemplateCollectionModel}, then a <code>java.util.ArrayList</code> is
     *       constructed from the subvariables, and each subvariable is unwrapped with
     *       the rules described here (recursive unwrapping).
     *   <li>If the object implements {@link TemplateHashModelEx}, then a
     *       <code>java.util.HashMap</code> is constructed from the subvariables, and each
     *       subvariable is unwrapped with the rules described here (recursive unwrapping).
     *   <li>Throw a <code>TemplateModelException</code>, because it doesn't know how to
     *       unwrap the object.
     * </ol>
     */
    public static Object unwrap(TemplateModel model) throws TemplateModelException {
        return unwrap(model, false);
    }

    /**
     * Same as {@link #unwrap(TemplateModel)}, but it doesn't throw exception 
     * if it doesn't know how to unwrap the model, but rather returns it as-is.
     * @since 2.3.14
     */
    public static Object permissiveUnwrap(TemplateModel model) throws TemplateModelException {
        return unwrap(model, true);
    }
    
    /**
     * @deprecated the name of this method is mistyped. Use 
     * {@link #permissiveUnwrap(TemplateModel)} instead.
     */
    @Deprecated
    public static Object premissiveUnwrap(TemplateModel model) throws TemplateModelException {
        return unwrap(model, true);
    }
    
    private static Object unwrap(TemplateModel model, boolean permissive) throws TemplateModelException {
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

    private static Object unwrap(TemplateModel model, TemplateModel nullModel, boolean permissive) throws TemplateModelException {
        if (model instanceof AdapterTemplateModel) {
            return ((AdapterTemplateModel) model).getAdaptedObject(OBJECT_CLASS);
        }
        if (model instanceof WrapperTemplateModel) {
            return ((WrapperTemplateModel) model).getWrappedObject();
        }
        if (model == nullModel) {
            return null;
        }
        if (model instanceof TemplateScalarModel) {
            return ((TemplateScalarModel) model).getAsString();
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
            int size = seq.size();
            ArrayList list = new ArrayList(size);
            for (int i = 0; i < size; ++i) {
                list.add(unwrap(seq.get(i), nullModel, permissive));
            }
            return list;
        }
        if (model instanceof TemplateCollectionModel) {
            TemplateCollectionModel coll = (TemplateCollectionModel) model;
            ArrayList list = new ArrayList();
            TemplateModelIterator it = coll.iterator();            
            while (it.hasNext()) {
                list.add(unwrap(it.next(), nullModel, permissive));
            }
            return list;
        }
        if (model instanceof TemplateHashModelEx) {
            TemplateHashModelEx hash = (TemplateHashModelEx) model;
            Map<Object, Object> map = new LinkedHashMap<>();
            if (model instanceof TemplateHashModelEx2) {
                KeyValuePairIterator kvps = ((TemplateHashModelEx2) model).keyValuePairIterator();
                while (kvps.hasNext()) {
                    KeyValuePair kvp = kvps.next();
                    map.put(
                            unwrap(kvp.getKey(), nullModel, permissive),
                            unwrap(kvp.getValue(), nullModel, permissive));
                }
            } else {
                TemplateModelIterator keys = hash.keys().iterator();
                while (keys.hasNext()) {
                    String key = (String) unwrap(keys.next(), nullModel, permissive); 
                    map.put(key, unwrap(hash.get(key), nullModel, permissive));
                }
            }
            return map;
        }
        if (permissive) {
            return model;
        }
        throw new TemplateModelException("Cannot deep-unwrap model of type " + model.getClass().getName());
    }
}