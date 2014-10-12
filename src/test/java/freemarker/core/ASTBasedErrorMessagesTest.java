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

package freemarker.core;

import java.util.Map;

import org.junit.Test;

import freemarker.test.TemplateTest;

public class ASTBasedErrorMessagesTest extends TemplateTest {
    
    @Test
    public void testOverloadSelectionError() {
        assertErrorContains("${overloads.m(null)}", "2.3.21", "overloaded");
    }
    
    @Test
    public void testInvalidRefBasic() {
        assertErrorContains("${foo}", "foo", "specify a default");
        assertErrorContains("${map[foo]}", "foo", "\\!map[", "specify a default");
    }
    
    @Test
    public void testInvalidRefDollar() {
        assertErrorContains("${$x}", "$x", "must not start with \"$\"", "specify a default");
        assertErrorContains("${map.$x}", "map.$x", "must not start with \"$\"", "specify a default");
    }

    @Test
    public void testInvalidRefAfterDot() {
        assertErrorContains("${map.foo.bar}", "map.foo", "\\!foo.bar", "after the last dot", "specify a default");
    }

    @Test
    public void testInvalidRefInSquareBrackets() {
        assertErrorContains("${map['foo']}", "map", "final [] step", "specify a default");
    }

    @Test
    public void testInvalidRefSize() {
        assertErrorContains("${map.size()}", "map.size", "?size", "specify a default");
        assertErrorContains("${map.length()}", "map.length", "?length", "specify a default");
    }

    @Override
    protected Object createDataModel() {
        Map<String, Object> dataModel = createCommonTestValuesDataModel();
        dataModel.put("overloads", new Overloads());
        return dataModel;
    }
    
    public static class Overloads {
        
        @SuppressWarnings("unused")
        public void m(String s) {}
        
        @SuppressWarnings("unused")
        public void m(int i) {}
        
    }
    
}
