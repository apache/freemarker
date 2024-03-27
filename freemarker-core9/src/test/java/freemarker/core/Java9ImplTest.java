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

package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Java9ImplTest {

    @Test
    public void testIsAccessibleAccordingToModuleExports() throws Exception {
        assertNotNull(_JavaVersions.JAVA_9);
        assertTrue(_JavaVersions.JAVA_9.isAccessibleAccordingToModuleExports(Document.class));
        assertFalse(_JavaVersions.JAVA_9.isAccessibleAccordingToModuleExports(getSomeInternalClass()));
        assertTrue(_JavaVersions.JAVA_9.isAccessibleAccordingToModuleExports(String[].class));
        assertTrue(_JavaVersions.JAVA_9.isAccessibleAccordingToModuleExports(int.class));
    }

    private static Class<?> getSomeInternalClass() throws SAXException, IOException, ParserConfigurationException,
            NoSuchMethodException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader("<a></a>")));

        Method internalClassMethod = document.getClass().getMethod("getDocumentElement");
        Class<?> internalClass = internalClassMethod.getDeclaringClass();
        assertThat(internalClass.getName(), Matchers.startsWith("com."));

        return internalClass;
    }
}
