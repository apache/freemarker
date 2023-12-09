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

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.dom.test.DOMLoader;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.Assert.fail;

public class DefaultObjectWrapperExtensionTest extends TemplateTest {

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        addToDataModel("doc", DOMLoader.toDOM("<doc><title>test</title></doc>").getDocumentElement());
    }

    @Test
    public void testWithExtensions() throws IOException, TemplateException {
        setConfiguration(newConfigurationBuilder()
                .objectWrapper(
                        new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
                                .extensions(DOMDefaultObjectWrapperExtension.INSTANCE)
                                .build()
                ));
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

        // TODO [FM3]: java.lang.IllegalAccessException: class org.apache.freemarker.core.model.impl.DefaultObjectWrapper
        // cannot access class com.sun.org.apache.xerces.internal.dom.ElementImpl (in module java.xml) because module java.xml does not export com.sun.org.apache.xerces.internal.dom to unnamed module @6aba2b86
        // But, org.w3c.dom.Document.getElementsByTagName is accessible, so we should call that instead.
        // assertOutput("${doc.getElementsByTagName('title').item(0).textContent}", "test");
    }

}
