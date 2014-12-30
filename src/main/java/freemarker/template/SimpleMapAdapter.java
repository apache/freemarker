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
import java.util.SortedMap;

import freemarker.core._DelayedJQuote;
import freemarker.core._TemplateModelException;
import freemarker.ext.util.WrapperTemplateModel;

/**
 * Adapts a {@link Map} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@link Map}, but build a hash specifically to
 * be used from a template, also consider using {@link SimpleHash} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link SimpleMapAdapter} is as thread-safe as the {@link Map} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these hashes (though of
 * course, a Java methods called from the template can violate this rule).
 * 
 * @since 2.3.22
 */
public class SimpleMapAdapter extends WrappingTemplateModel
        implements TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel,
        Serializable {

    private final Map map;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param map
     *            The map to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array. Has to be
     *            {@link ObjectWrapperAndUnwrapper} because of planned future features.
     */
    public static SimpleMapAdapter adapt(Map map, ObjectWrapperAndUnwrapper wrapper) {
        return new SimpleMapAdapter(map, wrapper);
    }
    
    private SimpleMapAdapter(Map map, ObjectWrapper wrapper) {
        super(wrapper);
        this.map = map;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        Object val;
        try {
            val = map.get(key);
        } catch (ClassCastException e) {
            throw new _TemplateModelException(
                    e, new Object[] {
                            "ClassCastException while getting Map entry with String key ",
                            new _DelayedJQuote(key)
                    });
        } catch (NullPointerException e) {
            throw new _TemplateModelException(
                    e, new Object[] {
                            "NullPointerException while getting Map entry with String key ",
                            new _DelayedJQuote(key)
                    });
        }
            
        if (val == null) {
            // Check for Character key if this is a single-character string.
            // In SortedMap-s, however, we can't do that safely, as it can cause ClassCastException.
            if (key.length() == 1 && !(map instanceof SortedMap)) {
                Character charKey = new Character(key.charAt(0));
                try {
                    val = map.get(charKey);
                    if (val == null && !(map.containsKey(key) || map.containsKey(charKey))) {
                        return null;
                    }
                } catch (ClassCastException e) {
                    throw new _TemplateModelException(
                            e, new Object[] {
                                    "Class casting exception while getting Map entry with Character key ",
                                    new _DelayedJQuote(charKey)
                            });
                } catch (NullPointerException e) {
                    throw new _TemplateModelException(
                            e, new Object[] {
                                    "NullPointerException while getting Map entry with Character key ",
                                    new _DelayedJQuote(charKey)
                            });
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
