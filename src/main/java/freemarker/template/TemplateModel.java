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

import freemarker.template.utility.ClassUtil;

/**
 * The common super-interface of the interfaces that stand for the FreeMarker Template Language (FTL) data types.
 * The template language only deals with {@link TemplateModel}-s, not directly with plain Java objects. (For example,
 * it doesn't understand {@link java.lang.Number}, but {@link TemplateNumberModel}.) This is why the
 * data-model (aka. the "template context" in other languages) is (automatically) mapped to a tree of
 * {@link TemplateModel}-s.
 * 
 * <p>Mapping the plain Java objects to {@link TemplateModel}-s (or the other way around sometimes) is the
 * responsibility of the {@link ObjectWrapper} (can be set via {@link Configuration#setObjectWrapper(ObjectWrapper)}).
 * But not all {@link TemplateModel}-s are for wrapping a plain object. For example, a value created within a template
 * is not made to wrap an earlier existing object; it's a value that has always existed in the template language's
 * domain. Users can also write {@link TemplateModel} implementations and put them directly into the data-model for
 * full control over how that object is seen from the template. Certain {@link TemplateModel} interfaces doesn't
 * even have equivalent in Java. For example the directive type ({@link TemplateDirectiveModel}) is like that.
 * 
 * <p>Because {@link TemplateModel} "subclasses" are all interfaces, a value in the template language can have multiple
 * types. However, to prevent ambiguous situations, it's not recommended to make values that implement more than one of
 * these types: string, number, boolean, date. The intended applications are like string+hash, string+method,
 * hash+sequence, etc.
 * 
 * @see ClassUtil#getFTLTypeDescription(TemplateModel)
 */
public interface TemplateModel {
    
    /**
     * A general-purpose object to represent nothing. It acts as
     * an empty string, false, empty sequence, empty hash, and
     * null-returning method model.
     */
    TemplateModel NOTHING = GeneralPurposeNothing.getInstance();
}
