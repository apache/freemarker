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

package freemarker.test;

import java.util.ArrayList;
import java.util.List;

import freemarker.cache.StringTemplateLoader;

public class MonitoredTemplateLoader extends StringTemplateLoader {
    
    private final List<String> templatesTried = new ArrayList<String>();
    
    @Override
    public Object findTemplateSource(String name) {
        templatesTried.add(name);
        return super.findTemplateSource(name);
    }

    public List<String> getTemplatesTried() {
        return templatesTried;
    }
    
    public void clear() {
        templatesTried.clear();
    }
    
    
}