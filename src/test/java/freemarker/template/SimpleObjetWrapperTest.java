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

package freemarker.template;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SimpleObjetWrapperTest {
    
    @Test
    public void testDoesNotAllowAPIBuiltin() throws TemplateModelException {
        SimpleObjectWrapper sow = new SimpleObjectWrapper(Configuration.VERSION_2_3_22);
        
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
    public void testCanWrapBasicTypes() throws TemplateModelException {
        for (Version version : new Version[] { Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_22 }) {
            SimpleObjectWrapper sow = new SimpleObjectWrapper(version);
            assertTrue(sow.wrap("s") instanceof TemplateScalarModel);
            assertTrue(sow.wrap(1) instanceof TemplateNumberModel);
            assertTrue(sow.wrap(true) instanceof TemplateBooleanModel);
            assertTrue(sow.wrap(new Date()) instanceof TemplateDateModel);
            assertTrue(sow.wrap(new ArrayList()) instanceof TemplateSequenceModel);
            assertTrue(sow.wrap(new String[0]) instanceof TemplateSequenceModel);
            assertTrue(sow.wrap(new ArrayList().iterator()) instanceof TemplateCollectionModel);
            assertTrue(sow.wrap(new HashSet()) instanceof TemplateSequenceModel);
            assertTrue(sow.wrap(new HashMap()) instanceof TemplateHashModelEx);
            assertNull(sow.wrap(null));
        }
    }
    
    @Test
    public void testWontWrapDOM() throws SAXException, IOException, ParserConfigurationException,
            TemplateModelException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader("<doc><sub a='1' /></doc>"));
        Document doc = db.parse(is);
        
        SimpleObjectWrapper sow = new SimpleObjectWrapper(Configuration.VERSION_2_3_22);
        try {
            sow.wrap(doc);
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsString("won't wrap"));
        }
    }
    
    @Test
    public void testWontWrapGenericObjects() {
        SimpleObjectWrapper sow = new SimpleObjectWrapper(Configuration.VERSION_2_3_22);
        try {
            sow.wrap(new File("/x"));
            fail();
        } catch (TemplateModelException e) {
            assertThat(e.getMessage(), containsString("won't wrap"));
        }
    }
    
}
