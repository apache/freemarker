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
import java.util.Map;

import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts a {@link Map} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@link Map}, but build a hash specifically to
 * be used from a template, also consider using {@link SimpleHash} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link SimpleMapAdapter} is as thread-safe as the {@link Map} that it wraps. Normally you only have
 * to consider read-only access, as the FreeMarker template language doesn't allow writing these hashes (though of
 * course, a Java methods called from the template can violate this rule).
 * 
 * @since 2.3.22
 */
public final class SimpleMapAdapter extends WrappingTemplateModel
        implements TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel,
        Serializable {

    private final Map map;

    public SimpleMapAdapter(Map map, ObjectWrapper wrapper) {
        super(wrapper);
        this.map = map;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object val = map.get(key);
        if (val == null) {
            if (key.length() == 1) {
                // Check for a Character key if this is a single-character string
                Character charKey = new Character(key.charAt(0));
                val = map.get(charKey);
                if (val == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                    return null;
                }
            } else if (!map.containsKey(key)) {
                return null;
            }
        }
        return wrap(val);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    public TemplateCollectionModel keys() {
        return new SimpleCollection(map.keySet(), getObjectWrapper());
    }

    public TemplateCollectionModel values() {
        return new SimpleCollection(map.values(), getObjectWrapper());
    }

    public Object getAdaptedObject(Class hint) {
        return map;
    }

    public Object getWrappedObject() {
        return map;
    }

    /**
     * Returns the {@code toString()} of the underlying {@link Map}.
     */
    public String toString() {
        return map.toString();
    }

}
