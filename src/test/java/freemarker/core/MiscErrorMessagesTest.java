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

import org.junit.Test;

import freemarker.cache.TemplateNameFormat;
import freemarker.template.Configuration;
import freemarker.test.TemplateTest;

public class MiscErrorMessagesTest extends TemplateTest {

    @Test
    public void stringIndexOutOfBounds() {
        assertErrorContains("${'foo'[10]}", "length", "3", "10", "String index out of");
    }
    
    @Test
    public void wrongTemplateNameFormat() {
        Configuration cfg = new Configuration();
        cfg.setTemplateNameFormat(TemplateNameFormat.DEFAULT_2_4_0);
        setConfiguration(cfg);
        
        assertErrorContains("<#include 'foo:/bar:baaz'>", "Malformed template name", "':'");
        assertErrorContains("<#include '../baaz'>", "Malformed template name", "root");
        assertErrorContains("<#include '\u0000'>", "Malformed template name", "\\u0000");
    }

    @Test
    public void numericalKeyHint() {
        assertErrorContains("${{}[10]}", "[]", "?api");
    }
    
}
