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

import java.io.IOException;

import org.junit.Test;

import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class StringLiteralInterpolationTest extends TemplateTest {

    @Test
    public void basics() throws IOException, TemplateException {
        assertOutput("<#assign x = 1>${'${x}'}", "1");
        assertOutput("<#assign x = 1>${'${x} ${x}'}", "1 1");
        assertOutput("<#assign x = 1>${'$\\{x}'}", "${x}");
        assertOutput("<#assign x = 1>${'$\\{x} $\\{x}'}", "${x} ${x}");
    }

    /**
     * Broken behavior for backward compatibility.
     */
    @Test
    public void legacyBug() throws IOException, TemplateException {
        assertOutput("<#assign x = 1>${'$\\{x} ${x}'}", "1 1");
        assertOutput("<#assign x = 1>${'${x} $\\{x}'}", "1 1");
    }

    @Test
    public void escaping() throws IOException, TemplateException {
        assertOutput("<#escape x as x?html><#assign x = '&'>${x} ${'${x}'}</#escape> ${x}", "&amp; &amp; &");
    }
    
}
