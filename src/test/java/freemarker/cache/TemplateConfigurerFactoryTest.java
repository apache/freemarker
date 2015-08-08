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
package freemarker.cache;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import freemarker.core.TemplateConfigurer;

public class TemplateConfigurerFactoryTest {

    @Test
    public void testCondition1() throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer tc = newTemplateConfigurer(1);
        
        TemplateConfigurerFactory tcf = new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.ftlx"), tc);
        
        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "x.ftlx", tc);
    }

    @Test
    public void testCondition2() throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer tc = newTemplateConfigurer(1);
        
        TemplateConfigurerFactory tcf = new ConditionalTemplateConfigurerFactory(
                new FileNameGlobMatcher("*.ftlx"),
                new ConditionalTemplateConfigurerFactory(
                        new FileNameGlobMatcher("x.*"), tc));
        
        assertNotApplicable(tcf, "x.ftl");
        assertNotApplicable(tcf, "y.ftlx");
        assertApplicable(tcf, "x.ftlx", tc);
    }

    @Test
    public void testMerging() throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer tc1 = newTemplateConfigurer(1);
        TemplateConfigurer tc2 = newTemplateConfigurer(2);
        TemplateConfigurer tc3 = newTemplateConfigurer(3);
        
        TemplateConfigurerFactory tcf = new MergingTemplateConfigurerFactory(
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.ftlx"), tc1),
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*a*.*"), tc2),
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*b*.*"), tc3));
        
        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "x.ftlx", tc1);
        assertApplicable(tcf, "a.ftl", tc2);
        assertApplicable(tcf, "b.ftl", tc3);
        assertApplicable(tcf, "a.ftlx", tc1, tc2);
        assertApplicable(tcf, "b.ftlx", tc1, tc3);
        assertApplicable(tcf, "ab.ftl", tc2, tc3);
        assertApplicable(tcf, "ab.ftlx", tc1, tc2, tc3);
        
        assertNotApplicable(new MergingTemplateConfigurerFactory(), "x.ftl");
    }

    @Test
    public void testFirstMatch() throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer tc1 = newTemplateConfigurer(1);
        TemplateConfigurer tc2 = newTemplateConfigurer(2);
        TemplateConfigurer tc3 = newTemplateConfigurer(3);
        
        FirstMatchTemplateConfigurerFactory tcf = new FirstMatchTemplateConfigurerFactory(
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.ftlx"), tc1),
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*a*.*"), tc2),
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*b*.*"), tc3));

        try {
            assertNotApplicable(tcf, "x.ftl");
        } catch (TemplateConfigurerFactoryException e) {
            assertThat(e.getMessage(), Matchers.containsString("x.ftl"));
        }
        tcf.setNoMatchErrorDetails("Test details");
        try {
            assertNotApplicable(tcf, "x.ftl");
        } catch (TemplateConfigurerFactoryException e) {
            assertThat(e.getMessage(), Matchers.containsString("Test details"));
        }
        
        tcf.setAllowNoMatch(true);
        
        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "x.ftlx", tc1);
        assertApplicable(tcf, "a.ftl", tc2);
        assertApplicable(tcf, "b.ftl", tc3);
        assertApplicable(tcf, "a.ftlx", tc1);
        assertApplicable(tcf, "b.ftlx", tc1);
        assertApplicable(tcf, "ab.ftl", tc2);
        assertApplicable(tcf, "ab.ftlx", tc1);
        
        assertNotApplicable(new FirstMatchTemplateConfigurerFactory().allowNoMatch(true), "x.ftl");
    }

    @Test
    public void testComplex() throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer tcA = newTemplateConfigurer(1);
        TemplateConfigurer tcBSpec = newTemplateConfigurer(2);
        TemplateConfigurer tcBCommon = newTemplateConfigurer(3);
        TemplateConfigurer tcHH = newTemplateConfigurer(4);
        TemplateConfigurer tcHtml = newTemplateConfigurer(5);
        TemplateConfigurer tcXml = newTemplateConfigurer(6);
        TemplateConfigurer tcNWS = newTemplateConfigurer(7);
        
        TemplateConfigurerFactory tcf = new MergingTemplateConfigurerFactory(
                new FirstMatchTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new PathGlobMatcher("a/**"), tcA),
                        new ConditionalTemplateConfigurerFactory(new PathGlobMatcher("b/**"),
                                new MergingTemplateConfigurerFactory(
                                    new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*"), tcBCommon),
                                    new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.s.*"), tcBSpec))))
                        .allowNoMatch(true),
                new FirstMatchTemplateConfigurerFactory(
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.hh"), tcHH),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.*h"), tcHtml),
                        new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.*x"), tcXml))
                        .allowNoMatch(true),
                new ConditionalTemplateConfigurerFactory(new FileNameGlobMatcher("*.nws.*"), tcNWS));
        
        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "b/x.ftl", tcBCommon);
        assertApplicable(tcf, "b/x.s.ftl", tcBCommon, tcBSpec);
        assertApplicable(tcf, "b/x.s.ftlh", tcBCommon, tcBSpec, tcHtml);
        assertApplicable(tcf, "b/x.s.nws.ftlx", tcBCommon, tcBSpec, tcXml, tcNWS);
        assertApplicable(tcf, "a/x.s.nws.ftlx", tcA, tcXml, tcNWS);
        assertApplicable(tcf, "a.hh", tcHH);
        assertApplicable(tcf, "a.nws.hh", tcHH, tcNWS);
    }

    @SuppressWarnings("boxing")
    private TemplateConfigurer newTemplateConfigurer(int id) {
        TemplateConfigurer tc = new TemplateConfigurer();
        tc.setCustomAttribute("id", id);
        tc.setCustomAttribute("contains" + id, true);
        return tc;
    }

    private void assertNotApplicable(TemplateConfigurerFactory tcf, String sourceName)
            throws IOException, TemplateConfigurerFactoryException {
        assertNull(tcf.get(sourceName, "dummy"));
    }

    private void assertApplicable(TemplateConfigurerFactory tcf, String sourceName, TemplateConfigurer... expectedTCs)
            throws IOException, TemplateConfigurerFactoryException {
        TemplateConfigurer mergedTC = tcf.get(sourceName, "dummy");
        List<String> mergedTCAttNames = Arrays.asList(mergedTC.getCustomAttributeNames());

        for (TemplateConfigurer expectedTC : expectedTCs) {
            Integer tcId = (Integer) expectedTC.getCustomAttribute("id");
            if (tcId == null) {
                fail("TemplateConfigurer-s must be created with newTemplateConfigurer(id) in this test");
            }
            if (!mergedTCAttNames.contains("contains" + tcId)) {
                fail("TemplateConfigurer with ID " + tcId + " is missing from the asserted value");
            }
        }
        
        for (String attName: mergedTCAttNames) {
            if (!containsCustomAttr(attName, expectedTCs)) {
                fail("The asserted TemplateConfigurer contains an unexpected custom attribute: " + attName);
            }
        }
        
        assertEquals(expectedTCs[expectedTCs.length - 1].getCustomAttribute("id"), mergedTC.getCustomAttribute("id"));
    }

    private boolean containsCustomAttr(String attName, TemplateConfigurer... expectedTCs) {
        for (TemplateConfigurer expectedTC : expectedTCs) {
            if (expectedTC.getCustomAttribute(attName) != null) {
                return true;
            }
        }
        return false;
    }
    
}
