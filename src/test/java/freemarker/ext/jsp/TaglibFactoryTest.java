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

package freemarker.ext.jsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

@RunWith(JUnit4.class)
public class TaglibFactoryTest {

    @Test
    public void testTldParser() throws Exception {
        URL url = getClass().getResource("test.tld");
        TaglibFactory.TldParser tldParser = new TaglibFactory.TldParser();
        InputSource is = new InputSource();
        InputStream input = url.openStream();
        is.setByteStream(input);
        is.setSystemId(url.toString());
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(tldParser);
        reader.setErrorHandler(tldParser);
        reader.parse(is);
        input.close();

        assertEquals(1, tldParser.getListeners().size());
        assertTrue(tldParser.getListeners().get(0) instanceof ExampleContextListener);

        Map tagsAndFunctions = tldParser.getTagsAndFunctions();
        assertEquals(4, tagsAndFunctions.size());

        JspTagModelBase tag = (JspTagModelBase) tagsAndFunctions.get("setStringAttributeTag");
        assertNotNull(tag);
        tag = (JspTagModelBase) tagsAndFunctions.get("setStringAttributeTag2");
        assertNotNull(tag);

        JstlFunctionModel function = (JstlFunctionModel) tagsAndFunctions.get("toUpperCase");
        assertNotNull(function);
        assertEquals("ABC", function.exec(Arrays.asList(new TemplateModel [] { new SimpleScalar("abc") })));
        function = (JstlFunctionModel) tagsAndFunctions.get("toUpperCase2");
        assertNotNull(function);
        assertEquals("ABC", function.exec(Arrays.asList(new TemplateModel [] { new SimpleScalar("abc") })));
    }

}
