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

import freemarker.test.TemplateTest;

public class ParseTimeParameterBIErrorMessagesTest extends TemplateTest {

    @Test
    public void test1() throws Exception {
        assertErrorContains("${true?choose}", "expecting", "\"(\"");
        assertErrorContains("${true?choose()}", "?choose", "2 parameters");
        assertErrorContains("${true?choose(1)}", "?choose", "2 parameters");
        assertOutput("${true?choose(1, 2)}", "1");
        assertErrorContains("${true?choose(1, 2, 3)}", "?choose", "2 parameters");
    }

}
