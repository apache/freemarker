package freemarker.core;

import freemarker.ext.dom.NodeModel;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
public class SiblingTest extends TemplateTest {

    @Before
    public void setUp() {
        try {
            InputSource is = new InputSource(getClass().getResourceAsStream("siblingDataModel.xml"));
            addToDataModel("doc", NodeModel.parse(is) );
        } catch (Exception e) {
            System.out.println("Exception while parsing the dataModel xml");
            e.printStackTrace();
        }
    }

    @Test
    public void testEmptyPreviousSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.name?previousSibling}";
        assertOutput(ftl, "\n    ");
    }

    @Test
    public void testNonEmptyPreviousSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.address?previousSibling}";
        assertOutput(ftl, "12th August");
    }

    @Test
    public void testEmptyNextSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.name?nextSibling}";
        assertOutput(ftl, "\n    ");
    }

    @Test
    public void testNonEmptyNextSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.dob?nextSibling}";
        assertOutput(ftl, "Chennai, India");
    }


    @Test
    public void testNullPreviousSibling() throws IOException, TemplateException {
        String ftl = "<#if doc.person?previousSibling??> " +
                        "previous is not null" +
                      "<#else>" +
                         "previous is null" +
                    "</#if>";
        assertOutput(ftl, "previous is null");
    }

    @Test
    public void testSignificantPreviousSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.name.@@previous_significant}";
        assertOutput(ftl, "male");
    }


    @Test
    public void testSignificantNextSibling() throws IOException, TemplateException {
        String ftl = "${doc.person.name.@@next_significant}";
        assertOutput(ftl, "12th August");
    }

    @Test
    public void testNullSignificantPreviousSibling() throws IOException, TemplateException {
        String ftl = "<#if doc.person.phone.@@next_significant?size == 0>" +
                "Next is null" +
                "<#else>" +
                "Next is not null" +
                "</#if>";
        assertOutput(ftl, "Next is null");

    }

    @Test
    public void testSkippingCommentNode() throws IOException, TemplateException {
        String ftl = "${doc.person.profession.@@previous_significant}";
        assertOutput(ftl, "Chennai, India");
    }

    @Test
    public void testSkippingEmptyCdataNode() throws IOException, TemplateException {
        String ftl = "${doc.person.hobby.@@previous_significant}";
        assertOutput(ftl, "Software Engineer");
    }

    @Test
    public void testValidCdataNode() throws IOException, TemplateException {
        String ftl = "${doc.person.phone.@@previous_significant}";
        assertOutput(ftl, "\n    this is a valid cdata\n    ");
    }
}
