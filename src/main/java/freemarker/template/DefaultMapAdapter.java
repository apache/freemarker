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
import freemarker.template.utility.ObjectWrapperWithAPISupport;

/**
 * Adapts a {@link Map} to the corresponding {@link TemplateModel} interface(s), most importantly to
 * {@link TemplateHashModelEx}. If you aren't wrapping an already existing {@link Map}, but build a hash specifically to
 * be used from a template, also consider using {@link SimpleHash} (see comparison there).
 * 
 * <p>
 * Thread safety: A {@link DefaultMapAdapter} is as thread-safe as the {@link Map} that it wraps is. Normally you only
 * have to consider read-only access, as the FreeMarker template language doesn't allow writing these hashes (though of
 * course, Java methods called from the template can violate this rule).
 * 
 * <p>
 * This adapter is used by {@link DefaultObjectWrapper} if its {@code useAdaptersForCollections} property is
 * {@code true}, which is the default when its {@code incompatibleImprovements} property is 2.3.22 or higher.
 * 
 * @since 2.3.22
 */
public class DefaultMapAdapter extends WrappingTemplateModel
        implements TemplateHashModelEx, AdapterTemplateModel, WrapperTemplateModel, TemplateModelWithAPISupport,
        Serializable {

    private final Map map;

    /**
     * Factory method for creating new adapter instances.
     * 
     * @param map
     *            The map to adapt; can't be {@code null}.
     * @param wrapper
     *            The {@link ObjectWrapper} used to wrap the items in the array.
     */
    public static DefaultMapAdapter adapt(Map map, ObjectWrapperWithAPISupport wrapper) {
        return new DefaultMapAdapter(map, wrapper);
    }
    
    private DefaultMapAdapter(Map map, ObjectWrapper wrapper) {
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
                    if (val == null) {
                        TemplateModel wrappedNull = wrap(null);
                        if (wrappedNull == null || !(map.containsKey(key) || map.containsKey(charKey))) {
                            return null;
                        } else {
                            return wrappedNull;
                        }
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
            } else {  // No char key fallback was possible
                TemplateModel wrappedNull = wrap(null);
                if (wrappedNull == null || !map.containsKey(key)) {
                    return null;
                } else {
                    return wrappedNull;
                }
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

    public TemplateModel getAPI() throws TemplateModelException {
        return ((ObjectWrapperWithAPISupport) getObjectWrapper()).wrapAsAPI(map);
    }

}
