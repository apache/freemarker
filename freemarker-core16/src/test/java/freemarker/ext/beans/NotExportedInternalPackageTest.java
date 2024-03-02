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

package freemarker.ext.beans;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModelEx;

/**
 * FREEMARKER-216: IllegalAccessException because of JEP 396 - Strongly Encapsulate JDK Internals by Default
 */
public class NotExportedInternalPackageTest {
    @Test
    public void java16InternalClassAvoidanceTest() throws Exception {
        BeansWrapper bw = new BeansWrapper();

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader("<a></a>")));
        TemplateHashModel documentModel = (TemplateHashModel) bw.wrap(document);

        Method internalClassMethod = document.getClass().getMethod("getDocumentElement");
        assertTrue(Modifier.isPublic(internalClassMethod.getModifiers()));
        assertThat(internalClassMethod.getDeclaringClass().getName(), Matchers.startsWith("com.")); // Internal class

        TemplateMethodModelEx methodModel = (TemplateMethodModelEx) documentModel.get("getDocumentElement");
        assertNotNull(methodModel);

        assertNotNull(methodModel.exec(List.of())); // No IllegalAccessException
    }
}
