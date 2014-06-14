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

package freemarker.ext.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.util.ModelFactory;
import freemarker.template.AdapterTemplateModel;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModelEx;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 */
public class RhinoScriptableModel implements TemplateHashModelEx, 
TemplateSequenceModel, AdapterTemplateModel, TemplateScalarModel, 
TemplateBooleanModel, TemplateNumberModel
{
    static final ModelFactory FACTORY = new ModelFactory() {
        public TemplateModel create(Object object, ObjectWrapper wrapper) {
            return new RhinoScriptableModel((Scriptable)object, 
                    (BeansWrapper)wrapper);
        }
    };
    
    private final Scriptable scriptable;
    private final BeansWrapper wrapper;
    
    public RhinoScriptableModel(Scriptable scriptable, BeansWrapper wrapper) {
        this.scriptable = scriptable;
        this.wrapper = wrapper;
    }
    
    public TemplateModel get(String key) throws TemplateModelException {
        Object retval = ScriptableObject.getProperty(scriptable, key);
        if(retval instanceof Function) {
            return new RhinoFunctionModel((Function)retval, scriptable, wrapper);
        }
        else {
            return wrapper.wrap(retval);
        }
    }
    
    public TemplateModel get(int index) throws TemplateModelException {
        Object retval = ScriptableObject.getProperty(scriptable, index);
        if(retval instanceof Function) {
            return new RhinoFunctionModel((Function)retval, scriptable, wrapper);
        }
        else {
            return wrapper.wrap(retval);
        }
    }
    
    public boolean isEmpty() {
        return scriptable.getIds().length == 0;
    }
    
    public TemplateCollectionModel keys() throws TemplateModelException {
        return (TemplateCollectionModel)wrapper.wrap(scriptable.getIds());
    }
    
    public int size() {
        return scriptable.getIds().length;
    }
    
    public TemplateCollectionModel values() throws TemplateModelException {
        Object[] ids = scriptable.getIds();
        Object[] values = new Object[ids.length];
        for (int i = 0; i < values.length; i++) {
            Object id = ids[i];
            if(id instanceof Number) {
                values[i] = ScriptableObject.getProperty(scriptable, 
                        ((Number)id).intValue());
            }
            else {
                values[i] = ScriptableObject.getProperty(scriptable, 
                        String.valueOf(id)); 
            }
        }
        return (TemplateCollectionModel)wrapper.wrap(values);
    }
    
    public boolean getAsBoolean() {
        return Context.toBoolean(scriptable);
    }
    
    public Number getAsNumber()  {
        return new Double(Context.toNumber(scriptable));
    }
    
    public String getAsString() {
        return Context.toString(scriptable);
    }
    
    Scriptable getScriptable() {
        return scriptable;
    }

    BeansWrapper getWrapper() {
        return wrapper;
    }

    public Object getAdaptedObject(Class hint) {
        // FIXME: This does LS3 conversion, which is not very useful for us. Like it won't convert to List, Map, etc.  
        try {
            return NativeJavaObject.coerceType(hint, scriptable);
        }
        catch(EvaluatorException e) {
            return NativeJavaObject.coerceType(Object.class, scriptable);
        }
    }
}
