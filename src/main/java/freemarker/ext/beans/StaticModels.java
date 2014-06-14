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

package freemarker.ext.beans;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * Utility class for instantiating {@link StaticModel} instances from
 * templates. If your template's data model contains an instance of
 * StaticModels (named, say <tt>StaticModels</tt>), then you can
 * instantiate an arbitrary StaticModel using get syntax (i.e.
 * <tt>StaticModels["java.lang.System"].currentTimeMillis()</tt>).
 */
class StaticModels extends ClassBasedModelFactory {
    
    StaticModels(BeansWrapper wrapper) {
        super(wrapper);
    }

    protected TemplateModel createModel(Class clazz) 
    throws TemplateModelException {
        return new StaticModel(clazz, getWrapper());
    }
}