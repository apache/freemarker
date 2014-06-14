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

import java.util.LinkedHashMap;
import java.util.Map;

import freemarker.template.TemplateModel;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _EnumModels extends ClassBasedModelFactory {

    public _EnumModels(BeansWrapper wrapper) {
        super(wrapper);
    }
    
    protected TemplateModel createModel(Class clazz) {
        Object[] obj = clazz.getEnumConstants();
        if(obj == null) {
            // Return null - it'll manifest itself as undefined in the template.
            // We're doing this rather than throw an exception as this way 
            // people can use someEnumModel?default({}) to gracefully fall back 
            // to an empty hash if they want to.
            return null;
        }
        Map map = new LinkedHashMap();
        for (int i = 0; i < obj.length; i++) {
            Enum value = (Enum) obj[i];
            map.put(value.name(), value);
        }
        return new SimpleMapModel(map, getWrapper());
    }
}
