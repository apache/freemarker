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

package freemarker.test.templatesuite.models;

public class BooleanVsStringMethods {
    
    public String expectsString(String s) {
        return s;
    }

    public boolean expectsBoolean(boolean b) {
        return b;
    }
    
    public String overloaded(String s) {
        return "String " + s;
    }
    
    public String overloaded(boolean s) {
        return "boolean " + s;
    }
    
}
