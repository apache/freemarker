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
import freemarker.test.TemplateTest;

public class WhitespaceStrippingTest extends TemplateTest {

    private final Configuration cfgStripWS = new Configuration(Configuration.VERSION_2_3_21);
    private final Configuration cfgNoStripWS = new Configuration(Configuration.VERSION_2_3_21);
    {
        cfgNoStripWS.setWhitespaceStripping(false);
    }

    @Test
    public void testBasics() throws Exception {
        assertOutput("<#assign x = 1>\n<#assign y = 2>\n${x}\n${y}", "1\n2", "\n\n1\n2");
        assertOutput(" <#assign x = 1> \n <#assign y = 2> \n${x}\n${y}", "1\n2", "  \n  \n1\n2");
    }

    @Test
    public void testFTLHeader() throws Exception {
        assertOutput("<#ftl>x", "x", "x");
        assertOutput("  <#ftl>  x", "  x", "  x");
        assertOutput("\n<#ftl>\nx", "x", "x");
        assertOutput("\n<#ftl>\t \nx", "x", "x");
        assertOutput("  \n \n  <#ftl> \n \n  x", " \n  x", " \n  x");
    }

    @Test
    public void testComment() throws Exception {
        assertOutput(" a <#-- --> b ", " a  b ", " a  b ");
        assertOutput(" a \n<#-- -->\n b ", " a \n b ", " a \n\n b ");
        // These are wrong, but needed for 2.3.0 compatibility:
        assertOutput(" a \n <#-- --> \n b ", " a \n  b ", " a \n  \n b ");
        assertOutput(" a \n\t<#-- --> \n b ", " a \n\t b ", " a \n\t \n b ");
    }
    
    private void assertOutput(String ftl, String expectedOutStripped, String expectedOutNonStripped)
            throws IOException, TemplateException {
        setConfiguration(cfgStripWS);
        assertOutput(ftl, expectedOutStripped);
        
        setConfiguration(cfgNoStripWS);
        assertOutput(ftl, expectedOutNonStripped);
    }

}
