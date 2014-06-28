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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class HeaderParsingTest extends TemplateOutputTest {

    private final Configuration cfgStripWS = new Configuration(new Version(2, 3, 21));
    private final Configuration cfgNoStripWS = new Configuration(new Version(2, 3, 21));
    {
        cfgNoStripWS.setWhitespaceStripping(false);
    }
    
    @Test
    public void test() throws IOException, TemplateException {
        assertOutput("<#ftl>text", "text", "text");
        assertOutput(" <#ftl> text", " text", " text");
        assertOutput("\n<#ftl>\ntext", "text", "text");
        assertOutput("\n \n\n<#ftl> \ntext", "text", "text");
        assertOutput("\n \n\n<#ftl>\n\ntext", "\ntext", "\ntext");
    }
    
    private void assertOutput(final String ftl, String expectedOutStripped, String expectedOutNonStripped)
            throws IOException, TemplateException {
        for (int i = 0; i < 4; i++) {
            String ftlPermutation = ftl;
            if ((i & 1) == 1) {
                ftlPermutation = ftlPermutation.replace("<#ftl>", "<#ftl encoding='utf-8'>");
            }
            if ((i & 2) == 2) {
                ftlPermutation = ftlPermutation.replace('<', '[').replace('>', ']');
            }
            
            assertOutput(ftlPermutation, expectedOutStripped, cfgStripWS);
            assertOutput(ftlPermutation, expectedOutNonStripped, cfgNoStripWS);
        }
    }
    
}
