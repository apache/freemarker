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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.freemarker.core.model.TemplateModel;

class EnumModels extends ClassBasedModelFactory {

    public EnumModels(DefaultObjectWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    protected TemplateModel createModel(Class clazz) {
        Object[] obj = clazz.getEnumConstants();
        if (obj == null) {
            // Return null - it'll manifest itself as undefined in the template.
            // We're doing this rather than throw an exception as this way 
            // people can use someEnumModel!{} to gracefully fall back 
            // to an empty hash if they want to.
            return null;
        }
        Map map = new LinkedHashMap();
        for (Object anObj : obj) {
            Enum value = (Enum) anObj;
            map.put(value.name(), value);
        }
        return new SimpleHash(map, getWrapper());
    }
}
