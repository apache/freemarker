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

package org.apache.freemarker.dom;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.dom.test.DOMLoader;
import org.apache.freemarker.test.TemplateTest;
import org.apache.freemarker.test.TestConfigurationBuilder;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class DefaultObjectWrapperExtensionTest extends TemplateTest {

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        addToDataModel("doc", DOMLoader.toDOM("<doc><title>test</title></doc>").getDocumentElement());
    }

    @Test
    public void testWithExtensions() throws IOException, TemplateException {
        setConfiguration(new TestConfigurationBuilder()
                .objectWrapper(
                        new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                                .extensions(DOMDefaultObjectWrapperExtension.INSTANCE)
                                .build()
                )
                .build());
        assertOutput("${doc.title}", "test");
    }

    @Test
    public void testWithoutExtensions() throws IOException, TemplateException {
        try {
            assertOutput("${doc.title}", "test");
            fail();
        } catch (TemplateException e) {
            // Expected
        }

        assertOutput("${doc.getElementsByTagName('title').item(0).textContent}", "test");
    }

}
