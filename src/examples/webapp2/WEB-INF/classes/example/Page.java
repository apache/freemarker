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

package example;

import java.util.*;

public class Page {
    private String template;
    private String forward;
    private Map root = new HashMap();

    public String getTemplate() {
        return template;
    }
    
    public void setTemplate(String template) {
        forward = null;
        this.template = template;
    }

    public void put(String name, Object value) {
        root.put(name, value);
    }
    
    public void put(String name, int value) {
        root.put(name, new Integer(value));
    }
    
    public void put(String name, double value) {
        root.put(name, new Double(value));
    }

    public void put(String name, boolean value) {
        root.put(name, Boolean.valueOf(value));
    }
    
    public Map getRoot() {
        return root;
    }
    
    public String getForward() {
        return forward;
    }

    public void setForward(String forward) {
        template = null;
        this.forward = forward;
    }

}
