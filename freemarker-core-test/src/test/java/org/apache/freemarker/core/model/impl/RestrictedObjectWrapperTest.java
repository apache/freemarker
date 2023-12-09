/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.*;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapperTest.TestBean;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.apache.freemarker.test.hamcerst.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class RestrictedObjectWrapperTest {

    @Test
    public void testBasics() throws TemplateException {
        RestrictedObjectWrapper ow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        testCustomizationCommonPart(ow);
        assertTrue(ow.wrap(Collections.emptyMap()) instanceof DefaultMapAdapter);
        assertTrue(ow.wrap(Collections.emptyList()) instanceof DefaultListAdapter);
        assertTrue(ow.wrap(new boolean[] { }) instanceof DefaultArrayAdapter);
        assertTrue(ow.wrap(new HashSet()) instanceof DefaultNonListCollectionAdapter);
    }

    @SuppressWarnings("boxing")
    private void testCustomizationCommonPart(RestrictedObjectWrapper ow) throws TemplateException {
        assertTrue(ow.wrap("x") instanceof SimpleString);
        assertTrue(ow.wrap(1.5) instanceof SimpleNumber);
        assertTrue(ow.wrap(new Date()) instanceof SimpleDate);
        assertEquals(TemplateBooleanModel.TRUE, ow.wrap(true));
        
        try {
            ow.wrap(new TestBean());
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsStringIgnoringCase("type"));
        }
    }

    @Test
    public void testDoesNotAllowAPIBuiltin() throws TemplateException {
        RestrictedObjectWrapper sow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();

        TemplateModelWithAPISupport map = (TemplateModelWithAPISupport) sow.wrap(new HashMap());
        try {
            map.getAPI();
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("?api"));
        }
    }

    @SuppressWarnings("boxing")
    @Test
    public void testCanWrapBasicTypes() throws ObjectWrappingException {
        RestrictedObjectWrapper sow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        assertTrue(sow.wrap("s") instanceof TemplateStringModel);
        assertTrue(sow.wrap(1) instanceof TemplateNumberModel);
        assertTrue(sow.wrap(true) instanceof TemplateBooleanModel);
        assertTrue(sow.wrap(new Date()) instanceof TemplateDateModel);
        assertTrue(sow.wrap(new ArrayList()) instanceof TemplateSequenceModel);
        assertTrue(sow.wrap(new String[0]) instanceof TemplateSequenceModel);
        assertTrue(sow.wrap(new ArrayList().iterator()) instanceof TemplateIterableModel);
        assertTrue(sow.wrap(new HashSet()) instanceof TemplateCollectionModel);
        assertTrue(sow.wrap(new HashMap()) instanceof TemplateHashModelEx);
        assertNull(sow.wrap(null));
    }

    @Test
    public void testWontWrapDOM() throws SAXException, IOException, ParserConfigurationException, TemplateException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader("<doc><sub a='1' /></doc>"));
        Document doc = db.parse(is);

        RestrictedObjectWrapper sow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        try {
            sow.wrap(doc);
            fail();
        } catch (TemplateException e) {
            assertThat(e.getMessage(), containsString("won't wrap"));
        }
    }

    @Test
    public void testWontWrapGenericObjects() {
        RestrictedObjectWrapper sow = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0).build();
        try {
            sow.wrap(new File("/x"));
            fail();
        } catch (ObjectWrappingException e) {
            assertThat(e.getMessage(), containsString("won't wrap"));
        }
    }

    @Test
    public void testCanBeBuiltOnlyOnce() {
        RestrictedObjectWrapper.Builder builder = new RestrictedObjectWrapper.Builder(Configuration.VERSION_3_0_0);
        builder.build();
        try {
            builder.build();
            fail();
        } catch (IllegalStateException e) {
            // Expected
        }
    }

}
