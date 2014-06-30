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

package freemarker.template.utility;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import freemarker.template.EmptyMap;

/**
 * Implementation of missing JDK 1.3 collection features for JDK 1.2
 * 
 * @deprecated Not needed anymore, as FreeMarker now requires higher than Java 1.3
 */
public class Collections12
{
    public static final Map EMPTY_MAP = new EmptyMap();
    
    private Collections12() {
    }

    public static Map singletonMap(Object key, Object value) {
        return Collections.singletonMap(key, value);
    }

    public static List singletonList(Object o) {
        return Collections.singletonList(o);
    }
    
}
