/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.freemarker.core.templateresolver;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.freemarker.core.ProcessingConfiguration;
import org.apache.freemarker.core.TemplateConfiguration;
import org.junit.Test;

public class TemplateConfigurationFactoryTest {
    
    @Test
    public void testCondition1() throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration tc = newTemplateConfiguration(1);
        
        TemplateConfigurationFactory tcf = new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.ftlx"), tc);

        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "x.ftlx", tc);
    }

    @Test
    public void testCondition2() throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration tc = newTemplateConfiguration(1);
        
        TemplateConfigurationFactory tcf = new ConditionalTemplateConfigurationFactory(
                new FileNameGlobMatcher("*.ftlx"),
                new ConditionalTemplateConfigurationFactory(
                        new FileNameGlobMatcher("x.*"), tc));

        assertNotApplicable(tcf, "x.ftl");
        assertNotApplicable(tcf, "y.ftlx");
        assertApplicable(tcf, "x.ftlx", tc);
    }

    @Test
    public void testMerging() throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration tc1 = newTemplateConfiguration(1);
        TemplateConfiguration tc2 = newTemplateConfiguration(2);
        TemplateConfiguration tc3 = newTemplateConfiguration(3);
        
        TemplateConfigurationFactory tcf = new MergingTemplateConfigurationFactory(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.ftlx"), tc1),
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*a*.*"), tc2),
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*b*.*"), tc3));

        assertNotApplicable(tcf, "x.ftl");
        assertApplicable(tcf, "x.ftlx", tc1);
        assertApplicable(tcf, "a.ftl", tc2);
        assertApplicable(tcf, "b.ftl", tc3);
        assertApplicable(tcf, "a.ftlx", tc1, tc2);
        assertApplicable(tcf, "b.ftlx", tc1, tc3);
        assertApplicable(tcf, "ab.ftl", tc2, tc3);
        assertApplicable(tcf, "ab.ftlx", tc1, tc2, tc3);
        
        assertNotApplicable(new MergingTemplateConfigurationFactory(), "x.ftl");
    }

    @Test
    public void testFirstMatch() throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration tc1 = newTemplateConfiguration(1);
        TemplateConfiguration tc2 = newTemplateConfiguration(2);
        TemplateConfiguration tc3 = newTemplateConfiguration(3);
        
        FirstMatchTemplateConfigurationFactory tcf = new FirstMatchTemplateConfigurationFactory(
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.ftlx"), tc1),
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*a*.*"), tc2),
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*b*.*"), tc3));

        try {
            assertNotApplicable(tcf, "x.ftl");
        } catch (TemplateConfigurationFactoryException e) {
            assertThat(e.getMessage(), containsString("x.ftl"));
        }
        tcf.setNoMatchErrorDetails("Test details");
        try {
            assertNotApplicable(tcf, "x.ftl");
        } catch (TemplateConfigurationFactoryException e) {
            assertThat(e.getMessage(), containsString("Test details"));
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
        
        assertNotApplicable(new FirstMatchTemplateConfigurationFactory().allowNoMatch(true), "x.ftl");
    }

    @Test
    public void testComplex() throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration tcA = newTemplateConfiguration(1);
        TemplateConfiguration tcBSpec = newTemplateConfiguration(2);
        TemplateConfiguration tcBCommon = newTemplateConfiguration(3);
        TemplateConfiguration tcHH = newTemplateConfiguration(4);
        TemplateConfiguration tcHtml = newTemplateConfiguration(5);
        TemplateConfiguration tcXml = newTemplateConfiguration(6);
        TemplateConfiguration tcNWS = newTemplateConfiguration(7);
        
        TemplateConfigurationFactory tcf = new MergingTemplateConfigurationFactory(
                new FirstMatchTemplateConfigurationFactory(
                        new ConditionalTemplateConfigurationFactory(new PathGlobMatcher("a/**"), tcA),
                        new ConditionalTemplateConfigurationFactory(new PathGlobMatcher("b/**"),
                                new MergingTemplateConfigurationFactory(
                                    new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*"), tcBCommon),
                                    new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.s.*"), tcBSpec))))
                        .allowNoMatch(true),
                new FirstMatchTemplateConfigurationFactory(
                        new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.hh"), tcHH),
                        new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.*h"), tcHtml),
                        new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.*x"), tcXml))
                        .allowNoMatch(true),
                new ConditionalTemplateConfigurationFactory(new FileNameGlobMatcher("*.nws.*"), tcNWS));

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
    private TemplateConfiguration newTemplateConfiguration(int id) {
        TemplateConfiguration.Builder tcb = new TemplateConfiguration.Builder();
        tcb.setCustomSetting("id", id);
        tcb.setCustomSetting("contains" + id, true);
        return tcb.build();
    }

    private void assertNotApplicable(TemplateConfigurationFactory tcf, String sourceName)
            throws IOException, TemplateConfigurationFactoryException {
        assertNull(tcf.get(sourceName, DummyTemplateLoadingSource.INSTANCE));
    }

    private void assertApplicable(TemplateConfigurationFactory tcf, String sourceName, TemplateConfiguration... expectedTCs)
            throws IOException, TemplateConfigurationFactoryException {
        TemplateConfiguration mergedTC = tcf.get(sourceName, DummyTemplateLoadingSource.INSTANCE);
        List<Serializable> mergedTCAttNames = new ArrayList<>(mergedTC.getCustomSettingsSnapshot(false).keySet());

        for (TemplateConfiguration expectedTC : expectedTCs) {
            Integer tcId = (Integer) expectedTC.getCustomSetting("id");
            if (tcId == null) {
                fail("TemplateConfiguration-s must be created with newTemplateConfiguration(id) in this test");
            }
            if (!mergedTCAttNames.contains("contains" + tcId)) {
                fail("TemplateConfiguration with ID " + tcId + " is missing from the asserted value");
            }
        }
        
        for (Serializable attKey: mergedTCAttNames) {
            if (!containsCustomAttr(attKey, expectedTCs)) {
                fail("The asserted TemplateConfiguration contains an unexpected custom attribute: " + attKey);
            }
        }
        
        assertEquals(expectedTCs[expectedTCs.length - 1].getCustomSetting("id"), mergedTC.getCustomSetting("id"));
    }

    private boolean containsCustomAttr(Serializable attKey, TemplateConfiguration... expectedTCs) {
        for (TemplateConfiguration expectedTC : expectedTCs) {
            if (expectedTC.getCustomSetting(attKey, ProcessingConfiguration.MISSING_VALUE_MARKER)
                    != ProcessingConfiguration.MISSING_VALUE_MARKER) {
                return true;
            }
        }
        return false;
    }
    
    @SuppressWarnings("serial")
    private static class DummyTemplateLoadingSource implements TemplateLoadingSource {
        private static DummyTemplateLoadingSource INSTANCE = new DummyTemplateLoadingSource();
    }
    
}
